package com.server.dpmcore.gathering.gatheringMember.domain.model

import com.server.dpmcore.gathering.exception.GatheringMemberException
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.member.member.domain.model.MemberId
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
    isChecked: Boolean = false,
    isJoined: Boolean = false,
    isInvitationSubmitted: Boolean = false,
    val createdAt: Instant? = null,
    completedAt: Instant? = null,
    updatedAt: Instant? = null,
    deletedAt: Instant? = null,
) {
    var isChecked: Boolean = isChecked
        private set

    var isJoined: Boolean = isJoined
        private set

    var isInvitationSubmitted: Boolean = isInvitationSubmitted
        private set

    var completedAt: Instant? = completedAt
        private set

    var updatedAt: Instant? = updatedAt
        private set

    var deletedAt: Instant? = deletedAt
        private set

    fun markAsChecked() {
        this.isChecked = true
        this.updatedAt = Instant.now()
    }

    fun markAsJoined(isJoined: Boolean) {
        this.isJoined = isJoined
        this.updatedAt = Instant.now()
    }

    fun gatheringParticipationSubmittedConfirm() {
//        TODO : 논의 필요, 이 로직 자체가 사용자에게는 응답이 필요하지 않아서 Exception을 발생시키는 것이 맞는지
        if (isInvitationSubmitted) {
            throw GatheringMemberException.AlreadySubmittedInvitationException()
        }
        id ?: throw GatheringMemberException.GatheringMemberIdRequiredException()

        this.isInvitationSubmitted = true
        this.updatedAt = Instant.now()
    }

    fun isConfirmed(): Boolean = completedAt != null

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
            "isChecked=$isChecked," +
            "isJoined=$isJoined," +
            "isInvitationSubmitted=$isInvitationSubmitted," +
            "createdAt=$createdAt," +
            "completedAt=$completedAt," +
            "updatedAt=$updatedAt," +
            "deletedAt=$deletedAt)"
}
