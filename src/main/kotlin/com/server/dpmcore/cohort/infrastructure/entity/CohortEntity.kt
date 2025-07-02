package com.server.dpmcore.cohort.infrastructure.entity

import com.server.dpmcore.member.memberCohort.infrastructure.entity.MemberCohortEntity
import com.server.dpmcore.team.infrastructure.entity.TeamEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
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

    @OneToMany(mappedBy = "cohort", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val teams: List<TeamEntity> = mutableListOf(),

    @OneToMany(mappedBy = "cohort", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val memberCohorts: List<MemberCohortEntity> = mutableListOf(),
)
