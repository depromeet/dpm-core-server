package com.server.dpmcore.member.memberOAuth.infrastructure.entity

import com.server.dpmcore.member.member.infrastructure.entity.MemberEntity
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "member_oauth")
class MemberOAuthEntity(
    @Id
    @Column(name = "member_oauth_id", nullable = false, updatable = false)
    val id: String,

    @Column(nullable = false, updatable = false, unique = true)
    val externalId: String,

    @Column(nullable = false)
    val provider: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val member: MemberEntity,
)
