package core.entity.member

import core.domain.authority.vo.AuthorityId
import core.domain.member.aggregate.MemberAuthority
import core.domain.member.vo.MemberAuthorityId
import core.domain.member.vo.MemberId
import core.entity.authority.AuthorityEntity
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
@Table(name = "member_authorities")
class MemberAuthorityEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_authority_id", nullable = false, updatable = false)
    val id: Long,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val member: MemberEntity,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authority_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val authority: AuthorityEntity,
    @Column(nullable = false, updatable = false)
    val grantedAt: Instant? = null,
    @Column
    val deletedAt: Instant? = null,
) {
    fun toDomain() =
        MemberAuthority(
            id = MemberAuthorityId(this.id),
            memberId = MemberId(this.member.id),
            authorityId = AuthorityId(this.authority.id),
            grantedAt = this.grantedAt,
            deletedAt = this.deletedAt,
        )
}
