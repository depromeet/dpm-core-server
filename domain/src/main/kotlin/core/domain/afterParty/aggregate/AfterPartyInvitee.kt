package core.domain.afterParty.aggregate

import core.domain.afterParty.vo.AfterPartyId
import core.domain.afterParty.vo.AfterPartyInviteeId
import core.domain.member.vo.MemberId
import java.time.Instant

/**
 * AfterPartyInvitee는 특정 회식(AfterParty)에 초대된 멤버 정보를 나타냅니다.
 *
 * - 멤버는 초대받은 회식에 대해 '참석' 또는 '불참석' 여부를 선택할 수 있습니다.
 * - 참석 여부가 확정되면 confirmedAt 시간이 기록됩니다.
 */
class AfterPartyInvitee(
    val id: AfterPartyInviteeId? = null,
    val afterPartyId: AfterPartyId,
    val memberId: MemberId,
    rsvpStatus: Boolean? = null,
    isAttended: Boolean? = null,
    val invitedAt: Instant,
    confirmedAt: Instant? = null,
) {
    var rsvpStatus: Boolean? = rsvpStatus
        private set

    var isAttended: Boolean? = isAttended
        private set

    var confirmedAt: Instant? = confirmedAt
        private set

    fun isConfirmed(): Boolean = confirmedAt != null

    fun isRsvpGoing(): Boolean = rsvpStatus == true

    fun confirm(rsvpStatus: Boolean): AfterPartyInvitee =
        AfterPartyInvitee(
            id = this.id,
            afterPartyId = this.afterPartyId,
            memberId = this.memberId,
            rsvpStatus = rsvpStatus,
            isAttended = this.isAttended,
            invitedAt = this.invitedAt,
            confirmedAt = Instant.now(),
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AfterPartyInvitee) return false
        return id == other.id
    }

    override fun toString(): String =
        "AfterPartyInvitee(id=$id, gatheringId=$afterPartyId, memberId=$memberId, " +
            "rsvpStatus=$rsvpStatus, invitedAt=$invitedAt, confirmedAt=$confirmedAt)"

    companion object {
        /**
         * AfterPartyInvitee를 생성합니다.
         *
         * @param afterPartyId 초대받은 회식의 ID, 회식 생성이 선행되어야 합니다
         * @param memberId 초대할 멤버의 ID
         * @param invitedAt 초대 시각, 기본적으로 AfterParty createdAt 시각과 동일하게 설정해줘야 합니다.
         * @return 생성된 AfterPartyInvitee 인스턴스
         *
         * @since 2026-01-25
         * @author junwon
         */
        fun create(
            afterPartyId: AfterPartyId,
            memberId: MemberId,
            /** 초대 시각, 기본적으로 AfterParty createdAt 시각과 동일하게 설정해줘야 합니다. */
            invitedAt: Instant,
        ): AfterPartyInvitee =
            AfterPartyInvitee(
                afterPartyId = afterPartyId,
                memberId = memberId,
                invitedAt = invitedAt,
            )
    }
}
