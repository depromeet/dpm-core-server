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
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "authority")
class AuthorityEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "authority_id", nullable = false, updatable = false)
    val id: Long,

    @Column(nullable = false)
    val name: String,

    @CreationTimestamp
    val createdAt: LocalDateTime,

    @UpdateTimestamp
    val updatedAt: LocalDateTime,

    @OneToMany(mappedBy = "authority", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val memberAuthorities: MutableList<MemberAuthorityEntity> = mutableListOf(),
)
