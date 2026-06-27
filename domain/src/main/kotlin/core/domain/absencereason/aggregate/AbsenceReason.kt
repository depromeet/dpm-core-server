package core.domain.absencereason.aggregate

import core.domain.absencereason.enums.AbsenceReasonStatus
import core.domain.absencereason.port.inbound.command.AbsenceReportCreateCommand
import core.domain.absencereason.vo.AbsenceReasonId
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import java.time.Instant

/**
 * 결석 사유서(AbsenceReason) 도메인 모델
 *
 * 디퍼가 특정 세션(Session)의 결석에 대해 제출한 사유서를 나타냅니다.
 * 제출 시 [AbsenceReasonStatus.PENDING] 상태로 생성되며, 운영진의 검토를 거쳐 승인/반려됩니다.
 */
class AbsenceReason(
    val id: AbsenceReasonId? = null,
    val sessionId: SessionId,
    val memberId: MemberId,
    contents: String,
    status: AbsenceReasonStatus,
    createdAt: Instant? = null,
    updatedAt: Instant? = null,
) {
    var contents: String = contents
        private set

    var status: AbsenceReasonStatus = status
        private set

    var createdAt: Instant? = createdAt
        private set

    var updatedAt: Instant? = updatedAt
        private set

    /**
     * 사유서를 재제출합니다. 검토 상태를 다시 [AbsenceReasonStatus.PENDING] 으로 되돌립니다.
     *
     * @param contents 새로운 결석 사유
     */
    fun resubmit(contents: String) {
        this.contents = contents
        this.status = AbsenceReasonStatus.PENDING
        this.updatedAt = Instant.now()
    }

    fun approve() {
        this.status = AbsenceReasonStatus.APPROVED
        this.updatedAt = Instant.now()
    }

    fun reject() {
        this.status = AbsenceReasonStatus.REJECTED
        this.updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbsenceReason) return false
        return id == other.id && sessionId == other.sessionId && memberId == other.memberId
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + sessionId.hashCode()
        result = 31 * result + memberId.hashCode()
        return result
    }

    override fun toString(): String =
        "AbsenceReason(id=$id, sessionId=$sessionId, memberId=$memberId, status=$status)"

    companion object {
        fun create(command: AbsenceReportCreateCommand): AbsenceReason =
            AbsenceReason(
                sessionId = command.sessionId,
                memberId = command.memberId,
                contents = command.contents,
                status = AbsenceReasonStatus.PENDING,
                createdAt = Instant.now(),
            )
    }
}
