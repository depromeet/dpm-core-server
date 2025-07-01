package com.server.dpmcore.member.member.infrastructure.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "member")
class MemberEntity(
    @Id
    @Column(name = "member_id", nullable = false, updatable = false)
    val id: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val part: String,

    @Column(nullable = false)
    val status: String,

    @CreationTimestamp
    val createdAt: LocalDateTime,

    @UpdateTimestamp
    val updatedAt: LocalDateTime,

    @Column(name = "deleted_at")
    val deletedAt: LocalDateTime? = null,
)
