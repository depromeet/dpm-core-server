package core.application.image.infrastructure

import core.application.image.application.exception.ImageStorageUnavailableException
import core.application.image.application.properties.R2StorageProperties
import core.domain.image.port.outbound.CopyStart
import core.domain.image.port.outbound.CopyStatus
import core.domain.image.port.outbound.DownloadedObject
import core.domain.image.port.outbound.ImageStoragePort
import core.domain.image.port.outbound.PreauthenticatedUrl
import core.domain.image.port.outbound.StoredObject
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Cloudflare R2(S3 호환) 구현. `image.provider=r2` 일 때만 등록되며 OCI 구현보다 우선한다.
 *
 * OCI 와 다른 점
 * - PAR 대신 presigned URL 을 쓴다. 서명 URL 은 회수할 수 없어 [revokeUrl] 은 아무것도 하지 않고, 만료 시각으로만 끝난다.
 *   parId 는 회수 대상이 없다는 표시로 무작위 값을 준다.
 * - 복사는 동기다. 응답을 받으면 끝난 것이므로 [startCopy] 가 준 id 의 [copyStatus] 는 항상 COMPLETED 다.
 * - 대상 if-none-match 대신 복사 전에 대상 HEAD 로 확인한다. 확정 키는 업로드마다 다르고 lease 를 가진 처리자만 복사하며,
 *   원본 조건(if-match)이 같은 바이트를 보장하므로 경합으로 덮어써도 같은 내용이다.
 *
 * URL 은 권한 그 자체라 로그에 남기지 않는다.
 */
@Primary
@Component
@ConditionalOnProperty(prefix = "image", name = ["provider"], havingValue = "r2")
class R2ImageStorageAdapter(
    private val properties: R2StorageProperties,
    private val clientProvider: R2ClientProvider,
    private val clock: Clock,
) : ImageStoragePort {
    private val logger = KotlinLogging.logger { }

    override fun createUploadUrl(
        objectKey: String,
        expiresAt: Instant,
    ): PreauthenticatedUrl =
        call("presign-put", objectKey) {
            val request =
                PutObjectPresignRequest
                    .builder()
                    .signatureDuration(signatureDuration(expiresAt))
                    .putObjectRequest(PutObjectRequest.builder().bucket(properties.bucket).key(objectKey).build())
                    .build()
            presigned(clientProvider.presigner().presignPutObject(request).url().toString(), expiresAt)
        }

    override fun createReadUrl(
        objectKey: String,
        expiresAt: Instant,
    ): PreauthenticatedUrl = presignGet(objectKey, expiresAt, null)

    override fun createDownloadUrl(
        objectKey: String,
        expiresAt: Instant,
        fileName: String?,
    ): PreauthenticatedUrl = presignGet(objectKey, expiresAt, attachmentDisposition(fileName))

    override fun revokeUrl(parId: String) = Unit

    override fun download(
        objectKey: String,
        maxBytes: Long,
        target: Path,
    ): DownloadedObject? =
        call("download", objectKey, notFound = { null }) {
            try {
                downloadRange(objectKey, maxBytes, target)
            } catch (e: S3Exception) {
                if (e.statusCode() != RANGE_NOT_SATISFIABLE) throw e
                downloadEmpty(objectKey, target)
            }
        }

    /** 0..maxBytes(포함) 범위만 요청해 상한 + 1 바이트까지만 전송받는다. */
    private fun downloadRange(
        objectKey: String,
        maxBytes: Long,
        target: Path,
    ): DownloadedObject =
        clientProvider
            .client()
            .getObject(
                GetObjectRequest.builder().bucket(properties.bucket).key(objectKey).range("bytes=0-$maxBytes").build(),
            )
            .use { input ->
                val response = input.response()
                val etag = checkNotNull(response.eTag()) { "ETag 가 없습니다" }
                val size = Files.newOutputStream(target).use { output -> input.copyAtMost(output, maxBytes + 1) }
                DownloadedObject(etag, size, response.contentType(), response.contentEncoding())
            }

    /** 범위 요청이 416 이면 빈 객체일 때뿐이다. HEAD 로 길이 0 을 확인한 경우에만 빈 파일로 본다. */
    private fun downloadEmpty(
        objectKey: String,
        target: Path,
    ): DownloadedObject {
        val response = clientProvider.client().headObject(headRequest(objectKey))
        check(response.contentLength() == 0L) { "범위 요청이 416 인데 객체 길이가 0 이 아닙니다: ${response.contentLength()}" }
        Files.newOutputStream(target).close()
        return DownloadedObject(
            checkNotNull(response.eTag()) { "ETag 가 없습니다" },
            0L,
            response.contentType(),
            response.contentEncoding(),
        )
    }

    override fun head(objectKey: String): StoredObject? =
        call("head", objectKey, notFound = { null }) { headUnchecked(objectKey) }

    override fun startCopy(
        sourceKey: String,
        sourceEtag: String,
        destinationKey: String,
    ): CopyStart =
        call("copy", "$sourceKey -> $destinationKey", notFound = { CopyStart.PreconditionFailed }) {
            if (headOrNull(destinationKey) != null) return@call CopyStart.PreconditionFailed
            try {
                clientProvider.client().copyObject(
                    CopyObjectRequest
                        .builder()
                        .sourceBucket(properties.bucket)
                        .sourceKey(sourceKey)
                        // 검증한 바이트와 같은 객체일 때만 복사한다.
                        .copySourceIfMatch(sourceEtag)
                        .destinationBucket(properties.bucket)
                        .destinationKey(destinationKey)
                        .build(),
                )
                CopyStart.Started("$SYNC_COPY_PREFIX${UUID.randomUUID()}")
            } catch (e: S3Exception) {
                if (e.statusCode() !in PRECONDITION_STATUSES) throw e
                logger.info {
                    "R2 copy 조건 불일치: $sourceKey -> $destinationKey, status=${e.statusCode()}, requestId=${e.requestId()}"
                }
                CopyStart.PreconditionFailed
            }
        }

    /** 복사는 응답을 받으면 끝난 것이다. 이 구현이 준 id 가 아니면(OCI 시절 work request) 알 수 없으므로 진행 중으로 둔다. */
    override fun copyStatus(workRequestId: String): CopyStatus =
        if (workRequestId.startsWith(SYNC_COPY_PREFIX)) CopyStatus.COMPLETED else CopyStatus.IN_PROGRESS

    override fun delete(objectKey: String) {
        call("delete", objectKey, notFound = { }) {
            clientProvider.client().deleteObject(
                DeleteObjectRequest.builder().bucket(properties.bucket).key(objectKey).build(),
            )
        }
    }

    private fun presignGet(
        objectKey: String,
        expiresAt: Instant,
        contentDisposition: String?,
    ): PreauthenticatedUrl =
        call("presign-get", objectKey) {
            val getRequest =
                GetObjectRequest
                    .builder()
                    .bucket(properties.bucket)
                    .key(objectKey)
                    .apply { contentDisposition?.let { responseContentDisposition(it) } }
                    .build()
            val request =
                GetObjectPresignRequest
                    .builder()
                    .signatureDuration(signatureDuration(expiresAt))
                    .getObjectRequest(getRequest)
                    .build()
            presigned(clientProvider.presigner().presignGetObject(request).url().toString(), expiresAt)
        }

    private fun presigned(
        url: String,
        expiresAt: Instant,
    ) = PreauthenticatedUrl(parId = "$PRESIGNED_PREFIX${UUID.randomUUID()}", url = url, expiresAt = expiresAt)

    /** 서명 URL 은 최소 1초, 최대 7일이다. */
    private fun signatureDuration(expiresAt: Instant): Duration =
        Duration.between(clock.instant(), expiresAt).coerceIn(MIN_SIGNATURE_DURATION, MAX_SIGNATURE_DURATION)

    private fun headRequest(objectKey: String) =
        HeadObjectRequest.builder().bucket(
            properties.bucket,
        ).key(objectKey).build()

    private fun headUnchecked(objectKey: String): StoredObject {
        val response = clientProvider.client().headObject(headRequest(objectKey))
        return StoredObject(checkNotNull(response.eTag()) { "ETag 가 없습니다" }, checkNotNull(response.contentLength()))
    }

    private fun headOrNull(objectKey: String): StoredObject? =
        try {
            headUnchecked(objectKey)
        } catch (e: S3Exception) {
            if (e.statusCode() == NOT_FOUND) null else throw e
        }

    /** RFC 6266: ASCII 대체 이름(filename)과 UTF-8 원래 이름(filename*)을 같이 준다. OCI 구현과 같은 형식이다. */
    private fun attachmentDisposition(fileName: String?): String {
        if (fileName.isNullOrBlank()) return "attachment"
        val asciiFallback = fileName.map { if (isSafeAscii(it)) it else '_' }.joinToString("")
        return "attachment; filename=\"$asciiFallback\"; filename*=UTF-8''${encodeRfc5987(fileName)}"
    }

    private fun encodeRfc5987(value: String): String =
        value.toByteArray(Charsets.UTF_8).joinToString("") { byte ->
            val c = (byte.toInt() and 0xFF).toChar()
            if (isAttrChar(c)) c.toString() else "%%%02X".format(c.code)
        }

    private fun isSafeAscii(c: Char): Boolean = c.code in 0x20..0x7E && c != '"' && c != '\\'

    private fun isAttrChar(c: Char): Boolean = (c.isLetterOrDigit() && c.code < 0x80) || c in RFC5987_ATTR_CHARS

    private fun <T> call(
        operation: String,
        target: String,
        notFound: (() -> T)? = null,
        block: () -> T,
    ): T =
        try {
            block()
        } catch (e: ImageStorageUnavailableException) {
            throw e
        } catch (e: S3Exception) {
            if (e.statusCode() == NOT_FOUND && notFound != null) {
                notFound()
            } else {
                logger.error {
                    "R2 $operation 실패: $target, status=${e.statusCode()}, " +
                        "code=${e.awsErrorDetails()?.errorCode()}, requestId=${e.requestId()}"
                }
                throw ImageStorageUnavailableException()
            }
        } catch (e: Exception) {
            logger.error(e) { "R2 $operation 실패: $target" }
            throw ImageStorageUnavailableException()
        }

    /** [limit] 바이트까지만 옮기고 옮긴 수를 돌려준다. */
    private fun InputStream.copyAtMost(
        output: OutputStream,
        limit: Long,
    ): Long {
        val buffer = ByteArray(BUFFER_SIZE)
        var copied = 0L
        while (copied < limit) {
            val read = read(buffer, 0, minOf(buffer.size.toLong(), limit - copied).toInt())
            if (read < 0) break
            output.write(buffer, 0, read)
            copied += read
        }
        return copied
    }

    companion object {
        private const val NOT_FOUND = 404
        private const val RANGE_NOT_SATISFIABLE = 416
        private const val BUFFER_SIZE = 64 * 1024
        private const val PRESIGNED_PREFIX = "r2-presigned-"
        private const val SYNC_COPY_PREFIX = "r2-copy-"

        // 원본 ETag 불일치(412)만 복사 거절로 본다.
        private val PRECONDITION_STATUSES = setOf(409, 412)
        private val MIN_SIGNATURE_DURATION = Duration.ofSeconds(1)
        private val MAX_SIGNATURE_DURATION = Duration.ofDays(7)

        // RFC 5987 attr-char 중 영숫자를 뺀 나머지. 이 밖의 바이트는 %XX 로 쓴다.
        private const val RFC5987_ATTR_CHARS = "!#$&+-.^_`|~"
    }
}
