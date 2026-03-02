package core.domain.afterParty.aggregate

import core.domain.afterParty.enums.AfterPartyCategory
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.vo.MemberId
import java.time.Instant

/**
 * 회식 참여 조사를 위해 사용하는 도메인 모델입니다.
 *
 * - 각 회식에는 제목, 설명, 개최일, 카테고리 등이 포함됩니다.
 * - 회식에 초대된 멤버들은 AfterPartyInvitee로 관리됩니다.
 */
class AfterParty(
    val id: AfterPartyId? = null,
    val title: String,
    val description: String? = null,
    val category: AfterPartyCategory,
    val scheduledAt: Instant,
    val closedAt: Instant,
    val isApproved: Boolean,
    val authorMemberId: MemberId,
    val createdAt: Instant? = null,
    updatedAt: Instant? = null,
    val canEditAfterApproval: Boolean,
) {
    var updatedAt: Instant? = updatedAt
        private set

    fun update(
        title: String? = null,
        description: String? = null,
        scheduledAt: Instant? = null,
        closedAt: Instant? = null,
        category: AfterPartyCategory? = null,
        canEditAfterApproval: Boolean? = null,
    ): AfterParty =
        AfterParty(
            id = this.id,
            title = title ?: this.title,
            description = description ?: this.description,
            category = category ?: this.category,
            scheduledAt = scheduledAt ?: this.scheduledAt,
            closedAt = closedAt ?: this.closedAt,
            isApproved = this.isApproved,
            authorMemberId = this.authorMemberId,
            createdAt = this.createdAt,
            updatedAt = Instant.now(),
            canEditAfterApproval = canEditAfterApproval ?: this.canEditAfterApproval,
        )

    fun isClosed(): Boolean = Instant.now().isAfter(closedAt)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AfterParty) return false
        return id == other.id
    }

    override fun toString(): String =
        "AfterParty(id=$id, title='$title', description=$description, " +
            "category=$category, createdAt=$createdAt, updatedAt=$updatedAt)"

    companion object {
        fun create(
            title: String,
            description: String? = null,
            category: AfterPartyCategory,
            scheduledAt: Instant,
            closedAt: Instant,
            isApproved: Boolean = false,
            authorMemberId: MemberId,
            canEditAfterApproval: Boolean,
        ): AfterParty =
            AfterParty(
                title = title,
                description = description,
                category = category,
                scheduledAt = scheduledAt,
                closedAt = closedAt,
                isApproved = isApproved,
                authorMemberId = authorMemberId,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                canEditAfterApproval = canEditAfterApproval,
            )
    }
}
