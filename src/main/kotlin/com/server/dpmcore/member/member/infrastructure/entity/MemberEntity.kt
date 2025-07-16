package com.server.dpmcore.member.member.infrastructure.entity

import com.server.dpmcore.member.member.domain.model.Member
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.domain.model.MemberPart
import com.server.dpmcore.member.member.domain.model.MemberStatus
import com.server.dpmcore.member.memberAuthority.infrastructure.entity.MemberAuthorityEntity
import com.server.dpmcore.member.memberCohort.infrastructure.entity.MemberCohortEntity
import com.server.dpmcore.member.memberTeam.infrastructure.entity.MemberTeamEntity
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
    @Column
    val name: String? = null,
    @Column(nullable = false, unique = true)
    val email: String,
    @Column
    val part: String? = null,
    @Column(nullable = false)
    val status: String,
    @Column(nullable = false, updatable = false)
    val createdAt: Instant? = null,
    @Column(nullable = false)
    val updatedAt: Instant? = null,
    @Column
    val deletedAt: Instant? = null,
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val memberAuthorities: MutableList<MemberAuthorityEntity> = mutableListOf(),
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val memberCohorts: MutableList<MemberCohortEntity> = mutableListOf(),
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val memberTeams: MutableList<MemberTeamEntity> = mutableListOf(),
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
