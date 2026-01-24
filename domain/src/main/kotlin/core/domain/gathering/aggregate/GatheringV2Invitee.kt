package core.domain.gathering.aggregate

import core.domain.gathering.vo.GatheringV2Id
import core.domain.gathering.vo.GatheringV2InviteeId
import core.domain.member.vo.MemberId
import java.time.Instant

/**
 * GatheringV2Invitee는 특정 회식(GatheringV2)에 초대된 멤버 정보를 나타냅니다.
 *
 * - 멤버는 초대받은 회식에 대해 '참석' 또는 '불참석' 여부를 선택할 수 있습니다.
 * - 참석 여부가 확정되면 confirmedAt 시간이 기록됩니다.
 */
class GatheringV2Invitee(
    val id: GatheringV2InviteeId? = null,
    val gatheringId: GatheringV2Id,
    val memberId: MemberId,
    isParticipated: Boolean? = null,
    val invitedAt: Instant? = null,
    confirmedAt: Instant? = null,
) {
    var isParticipated: Boolean? = isParticipated
        private set

    var confirmedAt: Instant? = confirmedAt
        private set

    fun isConfirmed(): Boolean = confirmedAt != null

    fun isParticipating(): Boolean = isParticipated == true

    fun confirm(isParticipated: Boolean): GatheringV2Invitee =
        GatheringV2Invitee(
            id = this.id,
            gatheringId = this.gatheringId,
            memberId = this.memberId,
            isParticipated = isParticipated,
            invitedAt = this.invitedAt,
            confirmedAt = Instant.now(),
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GatheringV2Invitee) return false
        return id == other.id
    }

    override fun toString(): String =
        "GatheringV2Invitee(id=$id, gatheringId=$gatheringId, memberId=$memberId, " +
                "isParticipated=$isParticipated, invitedAt=$invitedAt, confirmedAt=$confirmedAt)"

    companion object {
        fun create(
            gatheringId: GatheringV2Id,
            memberId: MemberId,
        ): GatheringV2Invitee =
            GatheringV2Invitee(
                gatheringId = gatheringId,
                memberId = memberId,
                invitedAt = Instant.now(),
            )
    }
}
