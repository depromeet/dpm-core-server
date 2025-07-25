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
    val isJoined: Boolean = false,
    val createdAt: Instant? = null,
    completedAt: Instant? = null,
    updatedAt: Instant? = null,
    deletedAt: Instant? = null,
) {
    var isChecked: Boolean = isChecked
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

    fun isDeleted(): Boolean = deletedAt != null

    fun isConfirmed(): Boolean = completedAt != null

    fun canUpdateAttendance(): Boolean = !isConfirmed()

    /**
     * 참석 여부 체크를 수행합니다.
     * 이미 확정된 경우 예외를 발생시킵니다.
     */
    fun checkAttendance(now: Instant): GatheringMember {
        if (isConfirmed()) {
            throw GatheringMemberException.AlreadyConfirmedMemberException()
        }

        return GatheringMember(
            id = id,
            gatheringId = gatheringId,
            memberId = memberId,
            isChecked = true,
            completedAt = now,
            createdAt = createdAt,
            updatedAt = now,
            deletedAt = deletedAt,
        )
    }

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
}
