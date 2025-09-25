package core.entity.member

import core.domain.member.aggregate.Member
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.vo.MemberId
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "members")
class MemberEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false, updatable = false)
    val id: Long,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false, unique = true)
    val email: String,
    @Column
    val part: String? = null,
    @Column(nullable = false)
    val status: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant? = null,
    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val memberAuthorities: MutableList<MemberAuthorityEntity> = mutableListOf(),
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val memberCohorts: MutableList<MemberCohortEntity> = mutableListOf(),
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val memberTeams: MutableList<MemberTeamEntity> = mutableListOf(),
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val memberOAuths: MutableList<MemberOAuthEntity> = mutableListOf(),
) {
    fun toDomain(): Member =
        Member(
            id = MemberId(this.id),
            name = name,
            email = email,
            part = this.part?.let { MemberPart.valueOf(it) },
            status = MemberStatus.valueOf(this.status),
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            memberAuthorities = memberAuthorities.map { it.toDomain() }.toMutableList(),
            memberCohorts = memberCohorts.map { it.toDomain() }.toMutableList(),
            memberTeams = memberTeams.map { it.toDomain() }.toMutableList(),
        )

    companion object {
        fun from(domain: Member): MemberEntity =
            MemberEntity(
                id = domain.id?.value ?: 0L,
                name = domain.name,
                email = domain.email,
                part = domain.part?.name,
                status = domain.status.name,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
                deletedAt = domain.deletedAt,
            )
    }
}
