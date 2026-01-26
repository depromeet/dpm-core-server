package core.entity.gathering

import core.domain.gathering.aggregate.GatheringV2Invitee
import core.domain.gathering.vo.GatheringV2Id
import core.domain.gathering.vo.GatheringV2InviteeId
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
@Table(name = "gatherings_v2_invitees")
class GatheringV2InviteeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gathering_invitee_id", nullable = false, updatable = false)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gathering_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val gathering: GatheringV2Entity,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val member: MemberEntity,
    @Column(name = "is_participated")
    val isParticipated: Boolean? = null,
    @Column(name = "invited_at", nullable = false)
    val invitedAt: Instant,
    @Column(name = "confirmed_at")
    val confirmedAt: Instant? = null,
) {
    companion object {
        fun from(
            gatheringV2Invitee: GatheringV2Invitee,
            gatheringV2Entity: GatheringV2Entity,
            memberEntity: MemberEntity,
        ): GatheringV2InviteeEntity =
            GatheringV2InviteeEntity(
                id = gatheringV2Invitee.id?.value ?: 0,
                gathering = gatheringV2Entity,
                member = memberEntity,
                isParticipated = gatheringV2Invitee.isParticipated,
                invitedAt = gatheringV2Invitee.invitedAt,
            )
    }

    fun toDomain(): GatheringV2Invitee =
        GatheringV2Invitee(
            id = GatheringV2InviteeId(this.id),
            gatheringId = GatheringV2Id(this.gathering.id),
            memberId = MemberId(this.member.id),
            isParticipated = this.isParticipated,
            invitedAt = this.invitedAt,
            confirmedAt = this.confirmedAt,
        )

    fun from(gatheringV2Invitee: GatheringV2Invitee): GatheringV2InviteeEntity =
        GatheringV2InviteeEntity(
            id = this.id,
            gathering = this.gathering,
            member = this.member,
            isParticipated = gatheringV2Invitee.isParticipated,
            invitedAt = this.invitedAt,
            confirmedAt = gatheringV2Invitee.confirmedAt,
        )
}
