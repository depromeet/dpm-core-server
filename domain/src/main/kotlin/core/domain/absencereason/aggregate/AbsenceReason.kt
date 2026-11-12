package core.domain.absencereason.aggregate

import core.domain.absencereason.enums.AbsenceReasonStatus
import core.domain.absencereason.port.inbound.command.AbsenceReportCreateCommand
import core.domain.absencereason.vo.AbsenceReasonId
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * 결석 사유서(AbsenceReason) 도메인 모델
 *
 * 디퍼가 특정 세션(Session)의 결석에 대해 제출한 사유서를 나타냅니다.
 * 제출하면 [AbsenceReasonStatus.PENDING] 상태로 생성되며 운영진이 승인하거나 반려합니다.
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

    /** 조회 시점 이후 다시 승인·반려·재제출된 사유서에는 예전 요청을 적용하지 않는다. */
    val reviewVersion: String
        get() = "${requireNotNull(id).value}:$status:${updatedAt ?: createdAt}"

    /**
     * 사유서를 재제출합니다. [AbsenceReasonStatus.PENDING] 상태로 돌아갑니다.
     *
     * @param contents 새로운 결석 사유
     */
    fun resubmit(contents: String) {
        this.contents = contents
        this.status = AbsenceReasonStatus.PENDING
        this.updatedAt = nextUpdatedAt()
    }

    fun approve() {
        this.status = AbsenceReasonStatus.APPROVED
        this.updatedAt = nextUpdatedAt()
    }

    fun reject() {
        this.status = AbsenceReasonStatus.REJECTED
        this.updatedAt = nextUpdatedAt()
    }

    /** 심사 결과가 이전 상태로 돌아오거나 시스템 시각이 뒤로 가도 이전 심사 버전을 재사용하지 않는다. */
    private fun nextUpdatedAt(): Instant {
        val now = Instant.now().truncatedTo(ChronoUnit.MICROS)
        // MySQL TIMESTAMP(6) 정밀도에서도 연속 변경의 버전이 항상 달라지도록 한다.
        val previous = (updatedAt ?: createdAt)?.truncatedTo(ChronoUnit.MICROS) ?: return now
        return maxOf(now, previous.plus(1, ChronoUnit.MICROS))
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

    override fun toString(): String = "AbsenceReason(id=$id, sessionId=$sessionId, memberId=$memberId, status=$status)"

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
