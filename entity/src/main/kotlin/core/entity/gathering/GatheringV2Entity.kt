package core.entity.gathering

import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.enums.GatheringCategory
import core.domain.gathering.vo.GatheringV2Id
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
@Table(name = "gatherings_v2")
class GatheringV2Entity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gathering_id", nullable = false, updatable = false)
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
            gatheringV2: GatheringV2,
            authorMember: MemberEntity,
        ): GatheringV2Entity =
            GatheringV2Entity(
                id = gatheringV2.id?.value ?: 0,
                title = gatheringV2.title,
                description = gatheringV2.description,
                category = gatheringV2.category.name,
                scheduledAt = gatheringV2.scheduledAt,
                closedAt = gatheringV2.closedAt,
                member = authorMember,
                createdAt = gatheringV2.createdAt ?: Instant.now(),
                updatedAt = gatheringV2.updatedAt ?: Instant.now(),
                canEditAfterApproval = gatheringV2.canEditAfterApproval,
            )
    }

    fun toDomain(): GatheringV2 =
        GatheringV2(
            id = GatheringV2Id(this.id),
            title = this.title,
            description = this.description,
            category = GatheringCategory.valueOf(this.category),
            scheduledAt = this.scheduledAt,
            closedAt = this.closedAt,
            authorMemberId = MemberId(this.member.id),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            canEditAfterApproval = this.canEditAfterApproval,
        )
}
