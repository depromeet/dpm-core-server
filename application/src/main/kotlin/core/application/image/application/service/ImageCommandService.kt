package core.application.image.application.service

import core.application.image.application.dto.ImageUploadCompletion
import core.application.image.application.exception.ImageExceptionCode
import core.application.image.application.exception.ImageStorageUnavailableException
import core.application.image.application.exception.ImageUploadException
import core.application.image.application.exception.ImageVerificationBusyException
import core.application.image.application.exception.InvalidImageException
import core.application.image.application.properties.ImageStorageProperties
import core.application.image.application.validator.ImageValidator
import core.application.image.presentation.response.ImageUploadCreateResponse
import core.application.image.presentation.response.ImageUploadResponse
import core.domain.image.aggregate.Image
import core.domain.image.aggregate.ImageUpload
import core.domain.image.enums.ImageUploadStatus
import core.domain.image.port.outbound.CopyStart
import core.domain.image.port.outbound.CopyStatus
import core.domain.image.port.outbound.ImageStoragePort
import core.domain.image.port.outbound.ImageUploadPersistencePort
import core.domain.image.port.outbound.StoredObject
import core.domain.member.vo.MemberId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Semaphore

/**
 * 프론트 직접 업로드: URL 발급 → (프론트가 OCI 로 PUT) → complete 에서 검증·확정 키 복사 후 이미지 행 생성.
 *
 * 클라우드 호출을 DB 트랜잭션·잠금에 묶지 않는다. 처리 주체는 세션 행의 lease(token)로 정하고, 모든 상태 변경은 token 조건부 UPDATE 라
 * lease 를 잃은 처리자는 결과를 반영하지 못한다. 저장소 쪽 부수 효과는 DB 전이가 반영된 뒤에만 일으킨다.
 *
 * 확정 키(images/{uploadId})에는 쓰기 PAR 을 만들지 않고, 모든 복사는 같은 검증 ETag 를 원본 조건으로, 대상 if-none-match 로 낸다.
 * 그래서 확정 키에 객체가 있고 크기가 같으면 검증한 바이트이며, 복사 응답을 못 받았을 때도 확정 키를 확인하거나 같은 복사를 다시 내면 된다.
 */
@Service
class ImageCommandService(
    private val imageValidator: ImageValidator,
    private val imageStoragePort: ImageStoragePort,
    private val imageUploadPersistencePort: ImageUploadPersistencePort,
    private val properties: ImageStorageProperties,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger { }

    // 검증(내려받기 + 디코드)은 인스턴스당 한 건만. 대기열 없이 429 로 돌려보낸다.
    private val verificationPermit = Semaphore(1)

    fun createUpload(
        ownerMemberId: MemberId,
        contentType: String,
        size: Long,
        fileName: String? = null,
    ): ImageUploadCreateResponse {
        val type =
            imageValidator.parseContentType(contentType)
                ?: throw InvalidImageException(ImageExceptionCode.UNSUPPORTED_TYPE)
        if (size <= 0) throw InvalidImageException(ImageExceptionCode.EMPTY_FILE)
        if (size > ImageValidator.MAX_BYTES) throw InvalidImageException(ImageExceptionCode.FILE_TOO_LARGE)
        val originalFileName = imageValidator.normalizeFileName(fileName)

        val now = Instant.now(clock)
        val uploadId = UUID.randomUUID().toString()
        val stagingKey = ImageUpload.STAGING_KEY_PREFIX + uploadId
        val par = imageStoragePort.createUploadUrl(stagingKey, now.plus(properties.uploadUrlTtl))
        try {
            imageUploadPersistencePort.save(
                ImageUpload.create(
                    uploadId,
                    ownerMemberId,
                    type,
                    size,
                    par.parId,
                    par.expiresAt,
                    now,
                    originalFileName,
                ),
            )
        } catch (e: Exception) {
            runQuietly("세션 저장 실패 후 PAR 회수", uploadId) { imageStoragePort.revokeUrl(par.parId) }
            throw e
        }
        return ImageUploadCreateResponse(uploadId, par.url, par.expiresAt)
    }

    /** 같은 uploadId 로 여러 번 불러도 안전하다. 완료 후에는 같은 imageId 를, 거절 후에는 같은 오류를 돌려준다. */
    fun completeUpload(
        memberId: MemberId,
        uploadId: String,
    ): ImageUploadCompletion {
        val upload =
            uploadId
                .takeIf { isUuid(it) }
                ?.let { imageUploadPersistencePort.findById(it) }
                ?.takeIf { it.isOwnedBy(memberId) }
                ?: throw ImageUploadException(ImageExceptionCode.UPLOAD_NOT_FOUND)

        return when (upload.status) {
            ImageUploadStatus.COMPLETED -> ImageUploadCompletion.Completed(completedResponse(upload))
            ImageUploadStatus.REJECTED, ImageUploadStatus.FAILED -> throw ImageUploadException(failureCode(upload))
            ImageUploadStatus.EXPIRED -> throw ImageUploadException(ImageExceptionCode.UPLOAD_EXPIRED)
            ImageUploadStatus.PENDING, ImageUploadStatus.VERIFYING -> verify(upload)
            ImageUploadStatus.COPYING -> copy(upload)
        }
    }

    /**
     * 오래된 세션 정리. 미완료 세션을 끝내고 PAR·업로드 객체를 지운다. 한 건이 실패해도 나머지는 진행하며,
     * 실패한 행은 그대로 남아(parId 유지) 다음 실행에서 다시 시도된다.
     * 저장소를 쓸 수 없으면 남은 건도 같은 타임아웃을 반복하며 공유 스케줄러 스레드를 붙잡으므로 이번 실행을 멈춘다.
     */
    fun cleanUpStaleUploads(): Int {
        val now = Instant.now(clock)
        var cleaned = 0
        val stale =
            imageUploadPersistencePort.findStale(now.minus(properties.abandonedAfter), properties.cleanupBatchSize)
        for (upload in stale) {
            try {
                if (cleanUpStale(upload, now)) cleaned++
            } catch (e: ImageStorageUnavailableException) {
                logger.warn(e) { "이미지 저장소를 쓸 수 없어 정리를 멈춥니다(다음 실행에서 재시도): uploadId=${upload.id}" }
                break
            } catch (e: Exception) {
                logger.warn(e) { "오래된 이미지 업로드를 정리하지 못했습니다(다음 실행에서 재시도): uploadId=${upload.id}" }
            }
        }
        return cleaned
    }

    private fun verify(upload: ImageUpload): ImageUploadCompletion {
        val now = Instant.now(clock)
        if (!upload.isLeaseFree(now)) return ImageUploadCompletion.InProgress
        // 만료된 업로드는 새 검증을 시작하지 않는다(이미 COPYING 이후인 세션은 영향 없음).
        if (!now.isBefore(upload.expiresAt)) throw ImageUploadException(ImageExceptionCode.UPLOAD_EXPIRED)
        if (!verificationPermit.tryAcquire()) throw ImageVerificationBusyException()

        val token = UUID.randomUUID().toString()
        try {
            val leaseUntil = now.plus(properties.processingLease)
            if (!imageUploadPersistencePort.startVerification(upload.id, token, now, leaseUntil)) {
                return ImageUploadCompletion.InProgress
            }
            if (!verifyLeased(upload, token)) return ImageUploadCompletion.InProgress
        } finally {
            verificationPermit.release()
        }

        // 검증이 끝났으니 쓰기 PAR 은 더 필요 없다. 실패해도 완료 후 정리나 정리 작업이 다시 회수한다.
        upload.parId?.let { parId -> runQuietly("쓰기 PAR 회수", upload.id) { imageStoragePort.revokeUrl(parId) } }
        val validated = imageUploadPersistencePort.findById(upload.id) ?: return ImageUploadCompletion.InProgress
        return advanceCopy(validated, token)
    }

    /** VERIFYING lease 를 가진 상태에서 검증한다. COPYING 으로 넘어갔으면 true, lease 를 잃었으면 false. */
    private fun verifyLeased(
        upload: ImageUpload,
        token: String,
    ): Boolean {
        val inspection =
            try {
                inspect(upload)
            } catch (e: Exception) {
                runQuietly("검증 lease 반환", upload.id) {
                    imageUploadPersistencePort.releaseVerification(upload.id, token)
                }
                throw e
            }

        return when (inspection) {
            Inspection.NotUploaded -> {
                imageUploadPersistencePort.releaseVerification(upload.id, token)
                throw ImageUploadException(ImageExceptionCode.NOT_UPLOADED)
            }
            is Inspection.Rejected -> {
                if (!imageUploadPersistencePort.reject(upload.id, token, inspection.code.name)) return false
                runQuietly("거절된 업로드 정리", upload.id) { removeStaging(upload) }
                throw InvalidImageException(inspection.code)
            }
            is Inspection.Valid -> {
                val leaseUntil = Instant.now(clock).plus(properties.processingLease)
                imageUploadPersistencePort.markValidated(upload.id, token, inspection.etag, leaseUntil)
            }
        }
    }

    /** 업로드 객체를 상한 + 1 바이트까지 임시 파일로 받아 검증한다. 임시 파일은 어떤 경우에도 지운다. */
    private fun inspect(upload: ImageUpload): Inspection {
        val temp = Files.createTempFile("image-upload-", ".tmp")
        try {
            val downloaded =
                imageStoragePort.download(upload.stagingKey, ImageValidator.MAX_BYTES.toLong(), temp)
                    ?: return Inspection.NotUploaded
            val rejection =
                when {
                    downloaded.size > ImageValidator.MAX_BYTES -> ImageExceptionCode.FILE_TOO_LARGE
                    downloaded.size == 0L -> ImageExceptionCode.EMPTY_FILE
                    downloaded.size != upload.size -> ImageExceptionCode.SIZE_MISMATCH
                    // PUT 때의 Content-Type/Encoding 이 확정 객체에 그대로 남아 읽기 URL 응답에 실린다.
                    imageValidator.parseContentType(downloaded.contentType) != upload.contentType ||
                        !downloaded.contentEncoding.isNullOrBlank() -> ImageExceptionCode.CONTENT_TYPE_MISMATCH
                    else -> null
                }
            if (rejection != null) return Inspection.Rejected(rejection)

            imageValidator.validate(temp, upload.contentType.mimeType)
            return Inspection.Valid(downloaded.etag)
        } catch (e: InvalidImageException) {
            return Inspection.Rejected(e.imageCode)
        } finally {
            try {
                Files.deleteIfExists(temp)
            } catch (e: Exception) {
                logger.warn(e) { "검증용 임시 파일을 지우지 못했습니다: uploadId=${upload.id}" }
            }
        }
    }

    private fun copy(upload: ImageUpload): ImageUploadCompletion {
        val now = Instant.now(clock)
        if (!upload.isLeaseFree(now)) return ImageUploadCompletion.InProgress
        val token = UUID.randomUUID().toString()
        if (!imageUploadPersistencePort.acquireCopy(upload.id, token, now, now.plus(properties.processingLease))) {
            return ImageUploadCompletion.InProgress
        }
        // 가져오기 전 다른 처리자가 work request id 를 남겼을 수 있어 다시 읽는다.
        val current = imageUploadPersistencePort.findById(upload.id) ?: return ImageUploadCompletion.InProgress
        return advanceCopy(current, token)
    }

    /** COPYING lease([token])를 가진 상태에서 복사를 진행한다. 예상 못 한 실패면 lease 를 돌려놓고 던진다. */
    private fun advanceCopy(
        upload: ImageUpload,
        token: String,
    ): ImageUploadCompletion =
        try {
            advanceCopyLeased(upload, token)
        } catch (e: Exception) {
            // ImageUploadException 은 이미 FAILED 로 끝나 lease 가 비워진 뒤다.
            if (e !is ImageUploadException) releaseCopyQuietly(upload, token)
            throw e
        }

    private fun advanceCopyLeased(
        upload: ImageUpload,
        token: String,
    ): ImageUploadCompletion {
        val etag = checkNotNull(upload.etag) { "COPYING 세션에 검증 ETag 가 없습니다: ${upload.id}" }
        val workRequestId = upload.workRequestId
        // 확정 객체가 있으면 복사 상태와 무관하게 확정한다(상태 조회 권한·지연에 막히지 않게).
        imageStoragePort.head(upload.finalKey)?.let { return finalize(upload, token, it) }
        if (workRequestId != null) {
            return when (imageStoragePort.copyStatus(workRequestId)) {
                CopyStatus.IN_PROGRESS -> releaseCopy(upload, token)
                // 첫 HEAD 와 상태 조회 사이에 복사가 끝났을 수 있어 다시 본다. 그래도 없으면 단정하지 않고 503 으로 재시도하게 한다.
                CopyStatus.COMPLETED -> {
                    imageStoragePort.head(upload.finalKey)?.let { return finalize(upload, token, it) }
                    logger.error { "복사는 완료됐는데 확정 객체가 보이지 않습니다: uploadId=${upload.id}, workRequestId=$workRequestId" }
                    throw ImageStorageUnavailableException()
                }
                // 알고 있는 복사가 실패했고 확정 객체가 없다.
                CopyStatus.FAILED -> fail(upload, token, ImageExceptionCode.UPLOAD_FAILED)
            }
        }

        // 복사를 낸 적이 없거나, 냈지만 응답을 못 받아 work request id 가 없다. 조건부 복사라 다시 내도 안전하다.
        return when (val start = imageStoragePort.startCopy(upload.stagingKey, etag, upload.finalKey)) {
            is CopyStart.Started -> {
                if (!imageUploadPersistencePort.recordCopyWorkRequest(upload.id, token, start.workRequestId)) {
                    return ImageUploadCompletion.InProgress
                }
                if (imageStoragePort.copyStatus(start.workRequestId) == CopyStatus.COMPLETED) {
                    imageStoragePort.head(upload.finalKey)?.let { return finalize(upload, token, it) }
                }
                releaseCopy(upload, token)
            }
            // 확정 키가 이미 있으면(앞선 복사가 끝남) 그대로 확정하고, 아니면 원본이 바뀌었거나 사라진 것이다.
            CopyStart.PreconditionFailed ->
                imageStoragePort.head(upload.finalKey)?.let { finalize(upload, token, it) }
                    ?: fail(upload, token, ImageExceptionCode.SOURCE_CHANGED)
        }
    }

    private fun finalize(
        upload: ImageUpload,
        token: String,
        finalObject: StoredObject,
    ): ImageUploadCompletion {
        if (finalObject.size != upload.size) {
            logger.error {
                "확정 객체 크기가 검증한 크기와 다릅니다: uploadId=${upload.id}, expected=${upload.size}, actual=${finalObject.size}"
            }
            return fail(upload, token, ImageExceptionCode.UPLOAD_FAILED)
        }
        val image =
            Image.create(
                upload.ownerMemberId,
                upload.finalKey,
                upload.contentType,
                upload.size,
                Instant.now(clock),
                upload.originalFileName,
            )
        val saved =
            imageUploadPersistencePort.complete(upload.id, token, image) ?: return ImageUploadCompletion.InProgress

        runQuietly("완료된 업로드 정리", upload.id) {
            removeStaging(upload)
            imageUploadPersistencePort.clearParId(upload.id)
        }
        return ImageUploadCompletion.Completed(
            ImageUploadResponse(
                imageId = requireNotNull(saved.id) { "저장된 이미지에 id 가 없습니다" }.value,
                contentType = saved.contentType.mimeType,
                size = saved.size,
                fileName = saved.originalFileName,
            ),
        )
    }

    private fun fail(
        upload: ImageUpload,
        token: String,
        code: ImageExceptionCode,
    ): ImageUploadCompletion {
        if (!imageUploadPersistencePort.fail(upload.id, token, code.name)) return ImageUploadCompletion.InProgress
        runQuietly("실패한 업로드 정리", upload.id) { removeStaging(upload) }
        throw ImageUploadException(code)
    }

    private fun releaseCopy(
        upload: ImageUpload,
        token: String,
    ): ImageUploadCompletion {
        imageUploadPersistencePort.releaseCopy(upload.id, token)
        return ImageUploadCompletion.InProgress
    }

    private fun releaseCopyQuietly(
        upload: ImageUpload,
        token: String,
    ) = runQuietly("복사 lease 반환", upload.id) { imageUploadPersistencePort.releaseCopy(upload.id, token) }

    private fun cleanUpStale(
        upload: ImageUpload,
        now: Instant,
    ): Boolean =
        when (upload.status) {
            ImageUploadStatus.PENDING, ImageUploadStatus.VERIFYING ->
                imageUploadPersistencePort.expire(upload.id, now) && removeTerminal(upload, ImageUploadStatus.EXPIRED)
            // 복사 결과를 모르는 채로 지우지 않는다. 사용자 요청과 같은 경로로 진행시키고, 끝난 상태는 다음 실행에서 정리한다.
            ImageUploadStatus.COPYING -> {
                try {
                    copy(upload)
                } catch (e: ImageUploadException) {
                    // FAILED 로 끝났다. 다음 실행에서 정리한다.
                }
                false
            }
            ImageUploadStatus.COMPLETED -> {
                removeStaging(upload)
                imageUploadPersistencePort.clearParId(upload.id)
            }
            ImageUploadStatus.REJECTED, ImageUploadStatus.FAILED, ImageUploadStatus.EXPIRED ->
                removeTerminal(upload, upload.status)
        }

    private fun removeTerminal(
        upload: ImageUpload,
        status: ImageUploadStatus,
    ): Boolean {
        removeStaging(upload)
        // 응답을 못 받은 복사가 FAILED 뒤에 끝나 확정 키에 남았을 수 있다. 이미지 행은 COMPLETED 에서만 생기므로 참조되지 않는다.
        if (status == ImageUploadStatus.FAILED) imageStoragePort.delete(upload.finalKey)
        return imageUploadPersistencePort.deleteTerminal(upload.id)
    }

    /** 쓰기 PAR 회수와 업로드 객체 삭제. 둘 다 이미 없으면 성공으로 본다. */
    private fun removeStaging(upload: ImageUpload) {
        upload.parId?.let(imageStoragePort::revokeUrl)
        imageStoragePort.delete(upload.stagingKey)
    }

    private fun completedResponse(upload: ImageUpload): ImageUploadResponse =
        ImageUploadResponse(
            imageId = requireNotNull(upload.imageId) { "완료된 업로드에 imageId 가 없습니다: ${upload.id}" }.value,
            contentType = upload.contentType.mimeType,
            size = upload.size,
            fileName = upload.originalFileName,
        )

    private fun failureCode(upload: ImageUpload): ImageExceptionCode =
        ImageExceptionCode.entries.firstOrNull { it.name == upload.failureCode } ?: ImageExceptionCode.UPLOAD_FAILED

    private fun isUuid(value: String): Boolean =
        value.length == UUID_LENGTH && runCatching { UUID.fromString(value) }.isSuccess

    private inline fun runQuietly(
        action: String,
        uploadId: String,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (e: Exception) {
            logger.warn(e) { "$action 실패(정리 작업이 다시 시도): uploadId=$uploadId" }
        }
    }

    private sealed interface Inspection {
        data object NotUploaded : Inspection

        data class Rejected(
            val code: ImageExceptionCode,
        ) : Inspection

        data class Valid(
            val etag: String,
        ) : Inspection
    }

    companion object {
        private const val UUID_LENGTH = 36
    }
}
