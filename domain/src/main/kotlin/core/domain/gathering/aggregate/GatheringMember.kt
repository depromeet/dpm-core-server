package core.domain.gathering.aggregate

import core.domain.gathering.vo.GatheringId
import core.domain.gathering.vo.GatheringMemberId
import core.domain.member.vo.MemberId
import java.time.Instant

/**
 * GatheringMember는 특정 회식 차수(Gathering)에 참여하는 멤버 정보를 나타냅니다.
 *
 * - 멤버는 각 차수에 대해 '참석' 또는 '불참석' 여부를 선택할 수 있습니다.
 * - 운영진은 모든 멤버가 입력을 완료하면, 멤버 확정을 수행할 수 있습니다.
 * - 확정 이후에는 수정이 불가능합니다.
 */
class GatheringMember(
    val id: GatheringMemberId? = null,
    val memberId: MemberId,
    val gatheringId: GatheringId,
    isViewed: Boolean = false,
    isJoined: Boolean? = null,
    isInvitationSubmitted: Boolean = false,
    memo: String? = null,
    val createdAt: Instant? = null,
    completedAt: Instant? = null,
    updatedAt: Instant? = null,
    deletedAt: Instant? = null,
) {
    var isViewed: Boolean = isViewed
        private set

    var isJoined: Boolean? = isJoined
        private set

    var isInvitationSubmitted: Boolean = isInvitationSubmitted
        private set

    var memo: String? = memo
        private set

    var completedAt: Instant? = completedAt
        private set

    var updatedAt: Instant? = updatedAt
        private set

    var deletedAt: Instant? = deletedAt
        private set

    fun isConfirmed(): Boolean = completedAt != null

    /**
     * 회식 멤버의 회식 식별자와 매개변수로 전달된 회식 식별자가 일치하는지 확인합니다.
     *
     * @param gatheringId 확인할 회식 식별자
     * @return 일치하면 true, 그렇지 않으면 false
     * @author LeeHanEum
     * @since 2025.09.13
     */
    fun isGatheringIdMatches(gatheringId: GatheringId) = this.gatheringId == gatheringId

    fun isJoined(): Boolean = this.isJoined == true && this.deletedAt == null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GatheringMember) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    companion object {
        fun create(
            gatheringId: GatheringId,
            memberId: MemberId,
        ): GatheringMember =
            GatheringMember(
                gatheringId = gatheringId,
                memberId = memberId,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
    }

    override fun toString(): String =
        "GatheringMember(id=$id," +
            "gatheringId=$gatheringId," +
            "memberId=$memberId," +
            "isViewed=$isViewed," +
            "isJoined=$isJoined," +
            "isInvitationSubmitted=$isInvitationSubmitted," +
            "createdAt=$createdAt," +
            "completedAt=$completedAt," +
            "updatedAt=$updatedAt," +
            "deletedAt=$deletedAt)"
}
