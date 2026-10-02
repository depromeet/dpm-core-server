package core.persistence.absencereason.repository

import core.entity.absencereason.AbsenceReasonImageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AbsenceReasonImageJpaRepository : JpaRepository<AbsenceReasonImageEntity, Long> {
    fun findAllByAbsenceReasonIdOrderByDisplayOrderAsc(absenceReasonId: Long): List<AbsenceReasonImageEntity>

    fun findAllByAbsenceReasonIdIn(absenceReasonIds: Collection<Long>): List<AbsenceReasonImageEntity>

    fun findAllByImageIdIn(imageIds: Collection<Long>): List<AbsenceReasonImageEntity>

    fun existsByAbsenceReasonIdAndImageId(
        absenceReasonId: Long,
        imageId: Long,
    ): Boolean
}
