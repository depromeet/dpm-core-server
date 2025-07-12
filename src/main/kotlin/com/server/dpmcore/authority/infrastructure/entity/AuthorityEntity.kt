package com.server.dpmcore.authority.infrastructure.entity

import com.server.dpmcore.member.memberAuthority.infrastructure.entity.MemberAuthorityEntity
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
@Table(name = "authorities")
class AuthorityEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "authority_id", nullable = false, updatable = false)
    val id: Long,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false, updatable = false)
    val createdAt: Long,
    @Column(nullable = false)
    val updatedAt: Long,
    @OneToMany(mappedBy = "authority", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val memberAuthorities: MutableList<MemberAuthorityEntity> = mutableListOf(),
)
