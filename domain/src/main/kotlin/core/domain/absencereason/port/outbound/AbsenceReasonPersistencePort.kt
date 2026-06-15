package core.domain.absencereason.port.outbound

import core.domain.absencereason.aggregate.AbsenceReason

interface AbsenceReasonPersistencePort {
    fun save(absenceReason: AbsenceReason): AbsenceReason

    fun findBySessionIdAndMemberId(
        sessionId: Long,
        memberId: Long,
    ): AbsenceReason?

    fun findAllBySessionId(sessionId: Long): List<AbsenceReason>

    fun delete(absenceReason: AbsenceReason)
}
