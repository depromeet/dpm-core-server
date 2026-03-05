package core.entity.afterParty

import core.domain.afterParty.aggregate.AfterPartyInvitee
import core.domain.afterParty.vo.AfterPartyId
import core.domain.afterParty.vo.AfterPartyInviteeId
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
@Table(name = "after_party_invitees")
class AfterPartyInviteeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "after_party_invitee_id", nullable = false, updatable = false)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "after_party_id", foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val afterParty: AfterPartyEntity,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val member: MemberEntity,
    @Column(name = "rsvp_status")
    val rsvpStatus: Boolean? = null,
    @Column(name = "is_attended")
    val isAttended: Boolean? = null,
    @Column(name = "invited_at", nullable = false)
    val invitedAt: Instant,
    @Column(name = "confirmed_at")
    val confirmedAt: Instant? = null,
) {
    companion object {
        fun from(
            afterPartyInvitee: AfterPartyInvitee,
            afterPartyEntity: AfterPartyEntity,
            memberEntity: MemberEntity,
        ): AfterPartyInviteeEntity =
            AfterPartyInviteeEntity(
                id = afterPartyInvitee.id?.value ?: 0,
                afterParty = afterPartyEntity,
                member = memberEntity,
                rsvpStatus = afterPartyInvitee.rsvpStatus,
                invitedAt = afterPartyInvitee.invitedAt,
            )
    }

    fun toDomain(): AfterPartyInvitee =
        AfterPartyInvitee(
            id = AfterPartyInviteeId(this.id),
            afterPartyId = AfterPartyId(this.afterParty.id),
            memberId = MemberId(this.member.id),
            rsvpStatus = this.rsvpStatus,
            isAttended = this.isAttended,
            invitedAt = this.invitedAt,
            confirmedAt = this.confirmedAt,
        )

    fun from(afterPartyInvitee: AfterPartyInvitee): AfterPartyInviteeEntity =
        AfterPartyInviteeEntity(
            id = this.id,
            afterParty = this.afterParty,
            member = this.member,
            rsvpStatus = afterPartyInvitee.rsvpStatus,
            isAttended = afterPartyInvitee.isAttended,
            invitedAt = this.invitedAt,
            confirmedAt = afterPartyInvitee.confirmedAt,
        )
}
