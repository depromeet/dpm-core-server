package core.entity.member

import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.OAuthProvider
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
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
@Table(name = "member_oauth")
class MemberOAuthEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_oauth_id", nullable = false, updatable = false)
    val id: Long,
    @Column(name = "external_id", nullable = false, updatable = false, unique = true)
    val externalId: String,
    @Column(nullable = false)
    val provider: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val member: MemberEntity,
) {
    fun toDomain(): MemberOAuth =
        MemberOAuth(
            id = MemberOAuthId(id),
            externalId = externalId,
            provider = OAuthProvider.valueOf(provider),
            memberId = MemberId(member.id),
        )

    companion object {
        fun of(
            memberOAuth: MemberOAuth,
            member: Member,
        ): MemberOAuthEntity =
            MemberOAuthEntity(
                id = memberOAuth.id?.value ?: 0L,
                externalId = memberOAuth.externalId,
                provider = memberOAuth.provider.name,
                member = MemberEntity.from(member),
            )
    }
}
