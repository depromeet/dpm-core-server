package core.persistence.cohort.repository

import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.port.outbound.query.CohortTeamQueryModel
import core.domain.cohort.vo.CohortId
import org.jooq.DSLContext
import org.jooq.dsl.tables.records.CohortsRecord
import org.jooq.dsl.tables.references.COHORTS
import org.jooq.dsl.tables.references.TEAMS
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class CohortRepository(
    private val cohortJpaRepository: CohortJpaRepository,
    private val dsl: DSLContext,
) : CohortPersistencePort {
    // Badge snapshots may already have loaded a CohortEntity before the cohort lock was acquired.
    // Read current scalar values rather than reusing that persistence-context snapshot.
    override fun findAll(): List<Cohort> = dsl.selectFrom(COHORTS).fetch { it.toDomain() }

    override fun findById(cohortId: CohortId): Cohort? =
        dsl.selectFrom(COHORTS).where(COHORTS.COHORT_ID.eq(cohortId.value)).fetchOne()?.toDomain()

    override fun findByValue(value: String): Cohort? =
        dsl.selectFrom(COHORTS).where(COHORTS.VALUE.eq(value)).fetchOne()?.toDomain()

    override fun findActive(): Cohort? =
        dsl.selectFrom(COHORTS).where(COHORTS.IS_ACTIVE.eq(true)).fetchOne()?.toDomain()

    override fun save(cohort: Cohort): Cohort {
        val now = System.currentTimeMillis()
        val existing = cohort.id?.let(::findById)
        if (existing != null) {
            // Never merge a replacement entity with empty orphan-removal collections.
            dsl.update(COHORTS).set(COHORTS.VALUE, cohort.value)
                .set(COHORTS.IS_ACTIVE, cohort.isActive)
                .set(
                    COHORTS.ACTIVATED_AT,
                    (cohort.activatedAt ?: existing.activatedAt)?.atOffset(ZoneOffset.UTC)?.toLocalDateTime(),
                )
                .set(COHORTS.UPDATED_AT, now).where(COHORTS.COHORT_ID.eq(cohort.id!!.value)).execute()
            return requireNotNull(findById(cohort.id!!))
        }
        val entity =
            core.entity.cohort.CohortEntity(
                id = cohort.id?.value ?: 0L,
                value = cohort.value,
                isActive = cohort.isActive,
                activatedAt = cohort.activatedAt ?: existing?.activatedAt,
                createdAt = cohort.createdAt ?: existing?.createdAt ?: now,
                updatedAt = now,
            )
        return cohortJpaRepository.save(entity).toDomain()
    }

    override fun rename(
        cohortId: CohortId,
        value: String,
    ): Cohort {
        dsl.update(COHORTS).set(COHORTS.VALUE, value).set(COHORTS.UPDATED_AT, System.currentTimeMillis())
            .where(COHORTS.COHORT_ID.eq(cohortId.value)).execute()
        return requireNotNull(findById(cohortId))
    }

    override fun deactivateAll() {
        dsl.update(COHORTS).set(COHORTS.IS_ACTIVE, false).set(COHORTS.UPDATED_AT, System.currentTimeMillis())
            .where(COHORTS.IS_ACTIVE.eq(true)).execute()
    }

    override fun activate(cohortId: CohortId) {
        deactivateAll()
        dsl.update(COHORTS).set(COHORTS.IS_ACTIVE, true)
            .set(COHORTS.ACTIVATED_AT, LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
            .set(COHORTS.UPDATED_AT, System.currentTimeMillis())
            .where(COHORTS.COHORT_ID.eq(cohortId.value)).execute()
    }

    private fun CohortsRecord.toDomain() =
        Cohort(
            id = CohortId(cohortId!!),
            value = value!!,
            isActive = isActive!!,
            activatedAt = activatedAt?.toInstant(ZoneOffset.UTC),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    override fun findTeamsByCohortId(cohortId: CohortId): List<CohortTeamQueryModel> =
        dsl
            .select(TEAMS.TEAM_ID, TEAMS.NUMBER)
            .from(TEAMS)
            .where(TEAMS.COHORT_ID.eq(cohortId.value))
            .orderBy(TEAMS.NUMBER.asc(), TEAMS.TEAM_ID.asc())
            .fetch { CohortTeamQueryModel(id = it[TEAMS.TEAM_ID]!!, number = it[TEAMS.NUMBER]!!) }

    override fun deleteById(cohortId: CohortId) {
        cohortJpaRepository.deleteById(cohortId.value)
    }

    override fun existsByValue(value: String): Boolean = cohortJpaRepository.existsByValue(value)

    override fun hasAnyReference(cohortId: CohortId): Boolean {
        val cohortValue = cohortId.value
        return listOf(
            "member_cohorts",
            "member_cohort_activities",
            "member_activity_events",
            "teams",
            "sessions",
            "after_party_invite_tags",
        ).any {
                tableName ->
            dsl.fetchExists(
                dsl.selectOne()
                    .from(DSL.table(DSL.name(tableName)))
                    .where(DSL.field(DSL.name("cohort_id"), Long::class.java).eq(cohortValue)),
            )
        }
    }
}
