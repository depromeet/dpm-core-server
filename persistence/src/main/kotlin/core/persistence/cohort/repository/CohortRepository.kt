package core.persistence.cohort.repository

import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.vo.CohortId
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class CohortRepository(
    private val cohortJpaRepository: CohortJpaRepository,
    private val dsl: DSLContext,
) : CohortPersistencePort {
    override fun findAll(): List<Cohort> = cohortJpaRepository.findAll().map { it.toDomain() }

    override fun findById(cohortId: CohortId): Cohort? = cohortJpaRepository.findByIdOrNull(cohortId.value)?.toDomain()

    override fun findByValue(value: String): Cohort? = cohortJpaRepository.findByValue(value)?.toDomain()

    override fun save(cohort: Cohort): Cohort {
        val now = System.currentTimeMillis()
        val entity =
            core.entity.cohort.CohortEntity(
                id = cohort.id?.value ?: 0L,
                value = cohort.value,
                createdAt = cohort.createdAt ?: now,
                updatedAt = now,
            )
        return cohortJpaRepository.save(entity).toDomain()
    }

    override fun deleteById(cohortId: CohortId) {
        cohortJpaRepository.deleteById(cohortId.value)
    }

    override fun existsByValue(value: String): Boolean = cohortJpaRepository.existsByValue(value)

    override fun hasAnyReference(cohortId: CohortId): Boolean {
        val cohortValue = cohortId.value
        return listOf(
            "member_cohorts",
            "teams",
            "sessions",
            "after_party_invite_tags",
        ).any { tableName ->
            dsl.fetchExists(
                dsl
                    .selectOne()
                    .from(DSL.table(DSL.name(tableName)))
                    .where(DSL.field(DSL.name("cohort_id"), Long::class.java).eq(cohortValue)),
            )
        }
    }
}
