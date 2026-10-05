package core.domain.image.port.outbound

import core.domain.image.aggregate.Image
import core.domain.image.vo.ImageId

/** 이미지 행은 업로드 완료 트랜잭션([ImageUploadPersistencePort.complete])에서만 만든다. */
interface ImagePersistencePort {
    fun findById(imageId: ImageId): Image?

    fun findAllByIds(imageIds: List<ImageId>): List<Image>
}
