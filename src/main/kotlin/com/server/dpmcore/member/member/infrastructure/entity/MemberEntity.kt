package com.server.dpmcore.member.member.infrastructure.entity

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

    @Column(nullable = false)
    val part: String,

    @Column(nullable = false)
    val status: String,

    @Column(nullable = false, updatable = false)
    val createdAt: Long,

    @Column(nullable = false)
    val updatedAt: Long,

    @Column
    val deletedAt: Long? = null,

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val memberAuthorities: MutableList<MemberAuthorityEntity> = mutableListOf(),

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val memberCohorts: MutableList<MemberCohortEntity> = mutableListOf(),

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val memberTeams: MutableList<MemberTeamEntity> = mutableListOf(),
)
