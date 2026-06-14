package core.persistence.absencereason.repository

import core.domain.absencereason.aggregate.AbsenceReason
import core.domain.absencereason.port.outbound.AbsenceReasonPersistencePort
import core.entity.absencereason.AbsenceReasonEntity
import org.springframework.stereotype.Repository

@Repository
class AbsenceReasonRepository(
    private val absenceReasonJpaRepository: AbsenceReasonJpaRepository,
) : AbsenceReasonPersistencePort {
    override fun save(absenceReason: AbsenceReason): AbsenceReason =
        absenceReasonJpaRepository.save(AbsenceReasonEntity.from(absenceReason)).toDomain()

    override fun findBySessionIdAndMemberId(
        sessionId: Long,
        memberId: Long,
    ): AbsenceReason? =
        absenceReasonJpaRepository
            .findBySessionIdAndMemberId(sessionId, memberId)
            ?.toDomain()

    override fun delete(absenceReason: AbsenceReason) {
        val id = absenceReason.id?.value ?: return
        absenceReasonJpaRepository.deleteById(id)
    }
}
