package core.domain.image.port.outbound

import core.domain.image.aggregate.Image
import core.domain.image.vo.ImageId

interface ImagePersistencePort {
    fun save(image: Image): Image

    fun findById(imageId: ImageId): Image?
}
