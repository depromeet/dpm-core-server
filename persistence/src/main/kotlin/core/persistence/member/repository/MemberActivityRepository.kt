package core.persistence.member.repository

import core.domain.member.enums.MemberActivityStatus
import core.domain.member.port.outbound.MemberActivityPersistencePort
import core.domain.member.port.outbound.MemberCohortActivity
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class MemberActivityRepository(private val dsl: DSLContext) : MemberActivityPersistencePort {
    override fun lockCohorts() {
        dsl.select(COHORT_ID).from(DSL.table("cohorts")).orderBy(COHORT_ID).forUpdate().fetch()
    }

    override fun find(memberId: Long, cohortId: Long): MemberCohortActivity? =
        dsl.select(MEMBER_ID, COHORT_ID, STATUS, SUSPENDED_AT).from(ACTIVITIES)
            .where(MEMBER_ID.eq(memberId), COHORT_ID.eq(cohortId)).fetchOne()?.toActivity()

    override fun findAll(memberIds: List<Long>, cohortId: Long): Map<Long, MemberCohortActivity> =
        if (memberIds.isEmpty()) emptyMap() else dsl.select(MEMBER_ID, COHORT_ID, STATUS, SUSPENDED_AT).from(ACTIVITIES)
            .where(MEMBER_ID.`in`(memberIds), COHORT_ID.eq(cohortId)).fetch().map { it.toActivity() }.associateBy { it.memberId }

    override fun findByCohort(cohortId: Long): List<MemberCohortActivity> =
        dsl.select(MEMBER_ID, COHORT_ID, STATUS, SUSPENDED_AT).from(ACTIVITIES)
            .where(COHORT_ID.eq(cohortId)).orderBy(MEMBER_ID).fetch().map { it.toActivity() }

    override fun transition(memberId: Long, cohortId: Long, status: MemberActivityStatus, actorId: Long?) {
        val previous = find(memberId, cohortId)
        if (previous?.status == status) return
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val suspendedAt = previous?.suspendedAt?.let { LocalDateTime.ofInstant(it, ZoneOffset.UTC) }
            ?: now.takeIf { status == MemberActivityStatus.SUSPENDED }
        if (previous == null) {
            dsl.insertInto(ACTIVITIES).set(MEMBER_ID, memberId).set(COHORT_ID, cohortId)
                .set(STATUS, status.name).set(SUSPENDED_AT, suspendedAt).set(CREATED_AT, now).set(UPDATED_AT, now).execute()
        } else {
            dsl.update(ACTIVITIES).set(STATUS, status.name).set(SUSPENDED_AT, suspendedAt).set(UPDATED_AT, now)
                .where(MEMBER_ID.eq(memberId), COHORT_ID.eq(cohortId)).execute()
        }
        dsl.insertInto(EVENTS).set(MEMBER_ID, memberId).set(COHORT_ID, cohortId)
            .set(DSL.field("actor_member_id", Long::class.java), actorId)
            .set(DSL.field("previous_status", String::class.java), previous?.status?.name)
            .set(STATUS, status.name).set(CREATED_AT, now).execute()
    }

    private fun Record.toActivity() = MemberCohortActivity(
        get(MEMBER_ID)!!, get(COHORT_ID)!!, MemberActivityStatus.valueOf(get(STATUS)!!), get(SUSPENDED_AT)?.toInstant(ZoneOffset.UTC),
    )

    private companion object {
        val ACTIVITIES = DSL.table("member_cohort_activities")
        val EVENTS = DSL.table("member_activity_events")
        val MEMBER_ID = DSL.field("member_id", Long::class.java)
        val COHORT_ID = DSL.field("cohort_id", Long::class.java)
        val STATUS = DSL.field("status", String::class.java)
        val SUSPENDED_AT = DSL.field("suspended_at", LocalDateTime::class.java)
        val CREATED_AT = DSL.field("created_at", LocalDateTime::class.java)
        val UPDATED_AT = DSL.field("updated_at", LocalDateTime::class.java)
    }
}
