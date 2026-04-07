package core.entity.afterParty

import core.domain.afterParty.aggregate.AfterPartyInviteTag
import core.domain.afterParty.vo.AfterPartyId
import core.domain.afterParty.vo.AfterPartyInviteTagId
import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId
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
@Table(name = "after_party_invite_tags")
class AfterPartyInviteTagEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "after_party_invite_tag_id", nullable = false, updatable = false)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "after_party_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val afterParty: AfterPartyEntity,
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
            afterPartyInviteTag: AfterPartyInviteTag,
            afterPartyEntity: AfterPartyEntity,
        ): AfterPartyInviteTagEntity =
            AfterPartyInviteTagEntity(
                id = afterPartyInviteTag.id?.value ?: 0,
                afterParty = afterPartyEntity,
                cohortId = afterPartyInviteTag.cohortId.value,
                authorityId = afterPartyInviteTag.authorityId.value,
                tagName = afterPartyInviteTag.tagName,
                createdAt = afterPartyInviteTag.createdAt ?: Instant.now(),
            )
    }

    fun toDomain(): AfterPartyInviteTag =
        AfterPartyInviteTag(
            id = AfterPartyInviteTagId(this.id),
            afterPartyId = AfterPartyId(this.afterParty.id),
            cohortId = CohortId(this.cohortId),
            authorityId = AuthorityId(this.authorityId),
            tagName = this.tagName,
            createdAt = this.createdAt,
        )
}
