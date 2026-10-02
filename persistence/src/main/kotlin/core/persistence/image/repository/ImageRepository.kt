package core.persistence.image.repository

import core.domain.image.aggregate.Image
import core.domain.image.port.outbound.ImagePersistencePort
import core.domain.image.vo.ImageId
import core.entity.image.ImageEntity
import org.springframework.stereotype.Repository

@Repository
class ImageRepository(
    private val imageJpaRepository: ImageJpaRepository,
) : ImagePersistencePort {
    /** 호출자에 트랜잭션이 없으면 save 자체의 짧은 트랜잭션에서 커밋까지 끝난다. 커밋 실패도 여기서 던진다. */
    override fun save(image: Image): Image = imageJpaRepository.save(ImageEntity.from(image)).toDomain()

    override fun findById(imageId: ImageId): Image? =
        imageJpaRepository
            .findById(imageId.value)
            .orElse(null)
            ?.toDomain()
}
