package com.server.dpmcore.authority.infrastructure.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "authority")
class AuthorityEntity(
    @Id
    @Column(name = "authority_id", nullable = false, updatable = false)
    val id: String,

    @Column(nullable = false)
    val name: String,

    @CreationTimestamp
    val createdAt: LocalDateTime,

    @UpdateTimestamp
    val updatedAt: LocalDateTime,
)
