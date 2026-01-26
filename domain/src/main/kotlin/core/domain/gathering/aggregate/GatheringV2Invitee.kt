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
    val invitedAt: Instant,
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
        /**
         * GatheringV2Invitee를 생성합니다.
         *
         * @param gatheringV2Id 초대받은 회식의 ID, 회식 생성이 선행되어야 합니다
         * @param memberId 초대할 멤버의 ID
         * @param invitedAt 초대 시각, 기본적으로 GatheringV2 createdAt 시각과 동일하게 설정해줘야 합니다.
         * @return 생성된 GatheringV2Invitee 인스턴스
         *
         * @since 2026-01-25
         * @author junwon
         */
        fun create(
            gatheringV2Id: GatheringV2Id,
            memberId: MemberId,
            /** 초대 시각, 기본적으로 GatheringV2 createdAt 시각과 동일하게 설정해줘야 합니다. */
            invitedAt: Instant,
        ): GatheringV2Invitee =
            GatheringV2Invitee(
                gatheringId = gatheringV2Id,
                memberId = memberId,
                invitedAt = invitedAt,
            )
    }
}
