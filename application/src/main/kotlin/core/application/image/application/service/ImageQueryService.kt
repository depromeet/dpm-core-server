package core.application.image.application.service

import core.application.image.application.exception.ImageNotFoundException
import core.application.image.application.properties.ImageStorageProperties
import core.application.image.presentation.response.ImageUrlResponse
import core.domain.image.port.outbound.ImagePersistencePort
import core.domain.image.port.outbound.ImageStoragePort
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

/**
 * 소유자에게만 짧은 읽기 URL 을 준다. 남의 이미지와 없는 이미지는 같은 404 이며, 소유 확인 전에는 스토리지를 호출하지 않는다.
 * 바이트는 서버를 거치지 않는다. URL 을 가진 사람은 만료(기본 1분) 전까지 누구나 읽을 수 있다.
 */
@Service
class ImageQueryService(
    private val imagePersistencePort: ImagePersistencePort,
    private val imageStoragePort: ImageStoragePort,
    private val properties: ImageStorageProperties,
    private val clock: Clock,
) {
    fun getImage(
        memberId: MemberId,
        imageId: ImageId,
    ): ImageUrlResponse {
        val image =
            imagePersistencePort.findById(imageId)?.takeIf { it.isOwnedBy(memberId) }
                ?: throw ImageNotFoundException()

        val url = imageStoragePort.createReadUrl(image.objectKey, Instant.now(clock).plus(properties.readUrlTtl))
        return ImageUrlResponse(url.url, url.expiresAt)
    }
}
