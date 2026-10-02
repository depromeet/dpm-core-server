package core.application.image.application.service

import core.application.image.application.exception.ImageExceptionCode
import core.application.image.application.exception.InvalidImageException
import core.application.image.application.validator.ImageValidator
import core.application.image.presentation.response.ImageUploadResponse
import core.domain.image.aggregate.Image
import core.domain.image.port.outbound.ImagePersistencePort
import core.domain.image.port.outbound.ImageStoragePort
import core.domain.member.vo.MemberId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.Clock
import java.time.Instant

/**
 * 스토리지 업로드 후 메타데이터를 저장한다. 클라우드 호출을 DB 트랜잭션에 묶지 않도록 @Transactional 을 두지 않으며,
 * 메타데이터는 저장소 save 의 짧은 트랜잭션에서 커밋된다.
 * 저장(커밋 포함)이 실패하면 올린 객체를 지운다. 그 삭제까지 실패하면 객체가 버킷에 남으며 로그로만 알린다.
 */
@Service
class ImageCommandService(
    private val imageValidator: ImageValidator,
    private val imageStoragePort: ImageStoragePort,
    private val imagePersistencePort: ImagePersistencePort,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger { }

    fun upload(
        ownerMemberId: MemberId,
        file: MultipartFile,
    ): ImageUploadResponse {
        if (file.isEmpty) throw InvalidImageException(ImageExceptionCode.EMPTY_FILE)
        if (file.size > ImageValidator.MAX_BYTES) throw InvalidImageException(ImageExceptionCode.FILE_TOO_LARGE)

        // MultipartFile.size 를 믿지 않고 상한 + 1 바이트까지만 읽는다.
        val content = file.inputStream.use { it.readNBytes(ImageValidator.MAX_BYTES + 1) }
        val contentType = imageValidator.validate(content, file.contentType)
        val image = Image.create(ownerMemberId, contentType, content.size.toLong(), Instant.now(clock))

        imageStoragePort.put(image.objectKey, content, contentType.mimeType)
        val saved =
            try {
                imagePersistencePort.save(image)
            } catch (e: Exception) {
                deleteUploadedObject(image.objectKey, e)
                throw e
            }

        return ImageUploadResponse(
            imageId = requireNotNull(saved.id) { "저장된 이미지에 id 가 없습니다" }.value,
            contentType = saved.contentType.mimeType,
            size = saved.size,
        )
    }

    private fun deleteUploadedObject(
        objectKey: String,
        cause: Exception,
    ) {
        try {
            imageStoragePort.delete(objectKey)
        } catch (cleanupFailure: Exception) {
            cause.addSuppressed(cleanupFailure)
            logger.error(cleanupFailure) { "이미지 메타데이터 저장 실패 후 객체 삭제도 실패해 버킷에 남았습니다: $objectKey" }
        }
    }
}
