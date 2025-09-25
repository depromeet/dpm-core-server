package core.entity.cohort

import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.vo.CohortId
import core.entity.member.MemberCohortEntity
import core.entity.team.TeamEntity
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
    @Column(name = "\"value\"", nullable = false, unique = true)
    val value: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Long,
    @Column(name = "updated_at", nullable = false)
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
