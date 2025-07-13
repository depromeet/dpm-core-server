package com.server.dpmcore.member.memberAuthority.infrastructure.entity

import com.server.dpmcore.authority.domain.model.AuthorityId
import com.server.dpmcore.authority.infrastructure.entity.AuthorityEntity
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.infrastructure.entity.MemberEntity
import com.server.dpmcore.member.memberAuthority.domain.MemberAuthority
import com.server.dpmcore.member.memberAuthority.domain.MemberAuthorityId
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
    val grantedAt: Long? = null,
    @Column
    val deletedAt: Long? = null,
) {
    fun toDomain() = MemberAuthority(
        id = MemberAuthorityId(this.id),
        memberId = MemberId(this.member.id),
        authorityId = AuthorityId(this.authority.id),
        grantedAt = this.grantedAt,
        deletedAt = this.deletedAt
    )
}
