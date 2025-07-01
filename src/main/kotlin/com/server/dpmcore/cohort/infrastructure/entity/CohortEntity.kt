package com.server.dpmcore.cohort.infrastructure.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "cohort")
class CohortEntity(
    @Id
    @Column(name = "cohort_id", nullable = false, updatable = false)
    val id: String,

    @Column(nullable = false, unique = true)
    val value: String,

    @CreationTimestamp
    val createdAt: LocalDateTime,

    @UpdateTimestamp
    val updatedAt: LocalDateTime,
)
