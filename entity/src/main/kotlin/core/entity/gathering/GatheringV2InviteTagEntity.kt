package core.entity.gathering

import core.domain.cohort.vo.CohortId
import core.domain.gathering.aggregate.GatheringV2InviteTag
import core.domain.gathering.vo.GatheringV2Id
import core.domain.gathering.vo.GatheringV2InviteTagId
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
@Table(name = "gatherings_v2_invite_tags")
class GatheringV2InviteTagEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gathering_invite_tag_id", nullable = false, updatable = false)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gathering_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val gathering: GatheringV2Entity,
    @Column(name = "cohort_id", nullable = false)
    val cohortId: Long,
    @Column(name = "authority_id", nullable = false)
    val authorityId: Long,
    @Column(name = "tag_name", nullable = false, length = 50)
    val tagName: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
) {
    companion object {
        fun from(
            gatheringV2InviteTag: GatheringV2InviteTag,
            gatheringV2Entity: GatheringV2Entity,
        ): GatheringV2InviteTagEntity =
            GatheringV2InviteTagEntity(
                id = gatheringV2InviteTag.id?.value ?: 0,
                gathering = gatheringV2Entity,
                cohortId = gatheringV2InviteTag.cohortId.value,
                authorityId = gatheringV2InviteTag.authorityId,
                tagName = gatheringV2InviteTag.tagName,
                createdAt = gatheringV2InviteTag.createdAt ?: Instant.now(),
            )
    }

    fun toDomain(): GatheringV2InviteTag =
        GatheringV2InviteTag(
            id = GatheringV2InviteTagId(this.id),
            gatheringId = GatheringV2Id(this.gathering.id),
            cohortId = CohortId(this.cohortId),
            authorityId = this.authorityId,
            tagName = this.tagName,
            createdAt = this.createdAt,
        )
}
