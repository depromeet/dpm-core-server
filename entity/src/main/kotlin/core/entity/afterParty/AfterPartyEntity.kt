package core.entity.afterParty

import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.enums.AfterPartyCategory
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.vo.MemberId
import core.entity.member.MemberEntity
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "after_party")
class AfterPartyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "after_party_id", nullable = false, updatable = false)
    val id: Long = 0,
    @Column(name = "title", nullable = false, length = 200)
    val title: String,
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,
    @Column(name = "category", nullable = false, length = 50)
    val category: String,
    @Column(name = "scheduled_at", nullable = false)
    val scheduledAt: Instant,
    @Column(name = "closed_at", nullable = false)
    val closedAt: Instant,
    @Column(name = "is_approved", nullable = false)
    val isApproved: Boolean,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val member: MemberEntity,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
    @Column(name = "can_edit_after_approval", nullable = false)
    val canEditAfterApproval: Boolean,
) {
    companion object {
        fun of(
            afterParty: AfterParty,
            authorMember: MemberEntity,
        ): AfterPartyEntity =
            AfterPartyEntity(
                id = afterParty.id?.value ?: 0,
                title = afterParty.title,
                description = afterParty.description,
                category = afterParty.category.name,
                scheduledAt = afterParty.scheduledAt,
                closedAt = afterParty.closedAt,
                isApproved = afterParty.isApproved,
                member = authorMember,
                createdAt = afterParty.createdAt ?: Instant.now(),
                updatedAt = afterParty.updatedAt ?: Instant.now(),
                canEditAfterApproval = afterParty.canEditAfterApproval,
            )
    }

    fun toDomain(): AfterParty =
        AfterParty(
            id = AfterPartyId(this.id),
            title = this.title,
            description = this.description,
            category = AfterPartyCategory.valueOf(this.category),
            scheduledAt = this.scheduledAt,
            closedAt = this.closedAt,
            isApproved = this.isApproved,
            authorMemberId = MemberId(this.member.id),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            canEditAfterApproval = this.canEditAfterApproval,
        )

    fun updateFrom(afterParty: AfterParty): AfterPartyEntity =
        AfterPartyEntity(
            id = this.id,
            title = afterParty.title,
            description = afterParty.description,
            category = afterParty.category.name,
            scheduledAt = afterParty.scheduledAt,
            closedAt = afterParty.closedAt,
            isApproved = afterParty.isApproved,
            member = this.member,
            createdAt = this.createdAt,
            updatedAt = Instant.now(),
            canEditAfterApproval = afterParty.canEditAfterApproval,
        )
}
