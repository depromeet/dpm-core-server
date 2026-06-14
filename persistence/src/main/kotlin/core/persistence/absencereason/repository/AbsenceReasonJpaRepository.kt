package core.persistence.absencereason.repository

import core.entity.absencereason.AbsenceReasonEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AbsenceReasonJpaRepository : JpaRepository<AbsenceReasonEntity, Long> {
    fun findBySessionIdAndMemberId(
        sessionId: Long,
        memberId: Long,
    ): AbsenceReasonEntity?
}
