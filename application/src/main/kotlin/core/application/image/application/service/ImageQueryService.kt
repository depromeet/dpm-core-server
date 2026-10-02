package core.application.image.application.service

import core.application.image.application.dto.ImageContent
import core.application.image.application.exception.ImageNotFoundException
import core.application.image.application.exception.ImageStorageUnavailableException
import core.application.image.application.validator.ImageValidator
import core.domain.image.aggregate.Image
import core.domain.image.port.outbound.ImagePersistencePort
import core.domain.image.port.outbound.ImageStoragePort
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

/**
 * 소유자만 원본을 받는다. 남의 이미지와 없는 이미지는 같은 404 이며, 소유 확인 전에는 스토리지를 호출하지 않는다.
 * 스토리지 호출 중 DB 트랜잭션을 잡지 않도록 @Transactional 을 두지 않는다.
 */
@Service
class ImageQueryService(
    private val imagePersistencePort: ImagePersistencePort,
    private val imageStoragePort: ImageStoragePort,
) {
    private val logger = KotlinLogging.logger { }

    fun getImage(
        memberId: MemberId,
        imageId: ImageId,
    ): ImageContent {
        val image =
            imagePersistencePort.findById(imageId)?.takeIf { it.isOwnedBy(memberId) }
                ?: throw ImageNotFoundException()

        return loadContent(image)
    }

    /** 접근 확인을 마친 이미지의 원본을 읽는다. 호출자는 DB 트랜잭션 밖이어야 한다. */
    fun loadContent(image: Image): ImageContent {
        val bytes = imageStoragePort.get(image.objectKey, minOf(image.size, ImageValidator.MAX_BYTES.toLong()))
        if (bytes.size.toLong() != image.size) {
            logger.error { "이미지 크기가 메타데이터와 다릅니다: imageId=${image.id}, expected=${image.size}, actual=${bytes.size}" }
            throw ImageStorageUnavailableException()
        }
        return ImageContent(image.contentType, bytes)
    }
}
