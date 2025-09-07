package com.server.dpmcore.cohort.infrastructure.entity

import com.server.dpmcore.cohort.domain.model.Cohort
import com.server.dpmcore.cohort.domain.model.CohortId
import com.server.dpmcore.member.memberCohort.infrastructure.entity.MemberCohortEntity
import com.server.dpmcore.team.infrastructure.entity.TeamEntity
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
@Table(name = "cohorts")
class CohortEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cohort_id", nullable = false, updatable = false)
    val id: Long,
    @Column(nullable = false, unique = true)
    val value: String,
    @Column(nullable = false, updatable = false)
    val createdAt: Long,
    @Column(nullable = false)
    val updatedAt: Long,
    @OneToMany(mappedBy = "cohort", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val teams: MutableList<TeamEntity> = mutableListOf(),
    @OneToMany(mappedBy = "cohort", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    val memberCohorts: MutableList<MemberCohortEntity> = mutableListOf(),
) {
    fun toDomain(): Cohort =
        Cohort(
            id = CohortId(id),
            value = value,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
