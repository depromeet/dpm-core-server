package core.persistence.image.repository

import core.domain.image.aggregate.Image
import core.domain.image.port.outbound.ImagePersistencePort
import core.domain.image.vo.ImageId
import org.springframework.stereotype.Repository

@Repository
class ImageRepository(
    private val imageJpaRepository: ImageJpaRepository,
) : ImagePersistencePort {
    override fun findById(imageId: ImageId): Image? =
        imageJpaRepository
            .findById(imageId.value)
            .orElse(null)
            ?.toDomain()

    override fun findAllByIds(imageIds: List<ImageId>): List<Image> {
        if (imageIds.isEmpty()) return emptyList()
        return imageJpaRepository.findAllById(imageIds.map { it.value }).map { it.toDomain() }
    }
}
