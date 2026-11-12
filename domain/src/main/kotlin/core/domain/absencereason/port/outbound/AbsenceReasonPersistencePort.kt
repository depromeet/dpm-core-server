package core.domain.absencereason.port.outbound

import core.domain.absencereason.aggregate.AbsenceReason

interface AbsenceReasonPersistencePort {
    fun save(absenceReason: AbsenceReason): AbsenceReason

    fun findBySessionIdAndMemberId(
        sessionId: Long,
        memberId: Long,
    ): AbsenceReason?

    fun findAllBySessionId(sessionId: Long): List<AbsenceReason>

    /** 한 세션의 수정 대상 중 승인된 사유서가 하나라도 있는지 조회한다. */
    fun existsApproved(
        sessionId: Long,
        memberIds: Collection<Long>,
    ): Boolean

    fun delete(absenceReason: AbsenceReason)
}
