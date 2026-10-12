package core.persistence.member.repository

import core.domain.member.port.outbound.MemberBadgePersistencePort
import core.domain.member.port.outbound.MemberBadgeState
import org.jooq.DSLContext
import org.jooq.dsl.tables.records.MemberBadgeStatesRecord
import org.jooq.dsl.tables.references.MEMBER_BADGE_MEMBERSHIPS
import org.jooq.dsl.tables.references.MEMBER_BADGE_STATES
import org.springframework.stereotype.Repository

@Repository
class MemberBadgeRepository(private val dsl: DSLContext) : MemberBadgePersistencePort {
    override fun findStates(cohortId: Long): List<MemberBadgeState> =
        dsl.selectFrom(MEMBER_BADGE_STATES).where(MEMBER_BADGE_STATES.COHORT_ID.eq(cohortId))
            .orderBy(MEMBER_BADGE_STATES.CARD).fetch().map(::state)

    override fun lockStates(cohortId: Long): List<MemberBadgeState> {
        // All writers, acknowledgement and first initialization use this same order.
        listOf("AT_RISK", "INCOMPLETE", "PENDING").forEach { card ->
            dsl.insertInto(MEMBER_BADGE_STATES)
                .set(MEMBER_BADGE_STATES.COHORT_ID, cohortId).set(MEMBER_BADGE_STATES.CARD, card)
                .set(MEMBER_BADGE_STATES.VERSION, 0L).set(MEMBER_BADGE_STATES.ACKNOWLEDGED_VERSION, 0L)
                .set(MEMBER_BADGE_STATES.TARGET_COUNT, 0).set(MEMBER_BADGE_STATES.INITIALIZED, false)
                .onDuplicateKeyUpdate().set(MEMBER_BADGE_STATES.COHORT_ID, cohortId).execute()
        }
        return dsl.selectFrom(MEMBER_BADGE_STATES).where(MEMBER_BADGE_STATES.COHORT_ID.eq(cohortId))
            .orderBy(MEMBER_BADGE_STATES.CARD).forUpdate().fetch().map(::state)
    }

    override fun findMembers(
        cohortId: Long,
        card: String,
    ): Set<Long> =
        dsl.select(MEMBER_BADGE_MEMBERSHIPS.MEMBER_ID).from(MEMBER_BADGE_MEMBERSHIPS)
            .where(MEMBER_BADGE_MEMBERSHIPS.COHORT_ID.eq(cohortId), MEMBER_BADGE_MEMBERSHIPS.CARD.eq(card))
            .fetch(MEMBER_BADGE_MEMBERSHIPS.MEMBER_ID).filterNotNull().toSet()

    override fun replaceMembers(
        cohortId: Long,
        card: String,
        previous: Set<Long>,
        members: Set<Long>,
    ) {
        val removed = previous - members
        if (removed.isNotEmpty()) {
            dsl.deleteFrom(MEMBER_BADGE_MEMBERSHIPS).where(
                MEMBER_BADGE_MEMBERSHIPS.COHORT_ID.eq(cohortId),
                MEMBER_BADGE_MEMBERSHIPS.CARD.eq(card),
                MEMBER_BADGE_MEMBERSHIPS.MEMBER_ID.`in`(removed),
            ).execute()
        }
        val added = members - previous
        if (added.isNotEmpty()) {
            val insert =
                dsl.insertInto(
                    MEMBER_BADGE_MEMBERSHIPS,
                    MEMBER_BADGE_MEMBERSHIPS.COHORT_ID,
                    MEMBER_BADGE_MEMBERSHIPS.CARD,
                    MEMBER_BADGE_MEMBERSHIPS.MEMBER_ID,
                )
            added.sorted().forEach { insert.values(cohortId, card, it) }
            insert.execute()
        }
    }

    override fun save(state: MemberBadgeState) {
        dsl.update(MEMBER_BADGE_STATES).set(MEMBER_BADGE_STATES.VERSION, state.version)
            .set(MEMBER_BADGE_STATES.ACKNOWLEDGED_VERSION, state.acknowledgedVersion)
            .set(
                MEMBER_BADGE_STATES.TARGET_COUNT,
                state.targetCount,
            ).set(MEMBER_BADGE_STATES.INITIALIZED, state.initialized)
            .where(MEMBER_BADGE_STATES.COHORT_ID.eq(state.cohortId), MEMBER_BADGE_STATES.CARD.eq(state.card)).execute()
    }

    private fun state(row: MemberBadgeStatesRecord) =
        MemberBadgeState(
            cohortId = requireNotNull(row.cohortId),
            card = requireNotNull(row.card),
            version = requireNotNull(row.version),
            acknowledgedVersion = requireNotNull(row.acknowledgedVersion),
            targetCount = requireNotNull(row.targetCount),
            initialized = requireNotNull(row.initialized),
        )
}
