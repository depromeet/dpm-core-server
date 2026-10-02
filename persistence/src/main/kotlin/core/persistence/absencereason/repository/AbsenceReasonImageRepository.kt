package core.persistence.absencereason.repository

import core.domain.absencereason.port.outbound.AbsenceReasonImagePersistencePort
import core.domain.image.vo.ImageId
import core.entity.absencereason.AbsenceReasonImageEntity
import org.springframework.stereotype.Repository

@Repository
class AbsenceReasonImageRepository(
    private val absenceReasonImageJpaRepository: AbsenceReasonImageJpaRepository,
) : AbsenceReasonImagePersistencePort {
    override fun findImageIds(absenceReasonId: Long): List<ImageId> =
        absenceReasonImageJpaRepository
            .findAllByAbsenceReasonIdOrderByDisplayOrderAsc(absenceReasonId)
            .map { ImageId(it.imageId) }

    override fun findImageIdsByAbsenceReasonIds(absenceReasonIds: List<Long>): Map<Long, List<ImageId>> {
        if (absenceReasonIds.isEmpty()) return emptyMap()
        return absenceReasonImageJpaRepository
            .findAllByAbsenceReasonIdIn(absenceReasonIds.distinct())
            .sortedBy { it.displayOrder }
            .groupBy({ it.absenceReasonId }, { ImageId(it.imageId) })
    }

    override fun findAbsenceReasonIdsByImageIds(imageIds: List<ImageId>): Map<ImageId, Long> {
        if (imageIds.isEmpty()) return emptyMap()
        return absenceReasonImageJpaRepository
            .findAllByImageIdIn(imageIds.map { it.value })
            .associate { ImageId(it.imageId) to it.absenceReasonId }
    }

    override fun exists(
        absenceReasonId: Long,
        imageId: ImageId,
    ): Boolean = absenceReasonImageJpaRepository.existsByAbsenceReasonIdAndImageId(absenceReasonId, imageId.value)

    /** PK 벌크 삭제 후 image_id 순으로 넣는다(재정렬 시 UNIQUE 충돌·범위 잠금·교착 회피). */
    override fun replaceImages(
        absenceReasonId: Long,
        imageIds: List<ImageId>,
    ) {
        deleteAll(absenceReasonId)
        if (imageIds.isEmpty()) return

        val links =
            imageIds
                .mapIndexed { order, imageId ->
                    AbsenceReasonImageEntity(
                        absenceReasonId = absenceReasonId,
                        imageId = imageId.value,
                        displayOrder = order,
                    )
                }.sortedBy { it.imageId }
        absenceReasonImageJpaRepository.saveAll(links)
        absenceReasonImageJpaRepository.flush()
    }

    override fun deleteAll(absenceReasonId: Long) {
        val linkIds =
            absenceReasonImageJpaRepository
                .findAllByAbsenceReasonIdOrderByDisplayOrderAsc(absenceReasonId)
                .map { it.id }
        if (linkIds.isNotEmpty()) absenceReasonImageJpaRepository.deleteAllByIdInBatch(linkIds)
    }
}
