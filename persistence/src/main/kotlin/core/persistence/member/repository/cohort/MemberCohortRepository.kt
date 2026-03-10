package core.persistence.member.repository.cohort

import core.domain.member.aggregate.MemberCohort
import core.domain.member.port.outbound.MemberCohortPersistencePort
import org.jooq.DSLContext
import org.jooq.dsl.tables.references.MEMBER_COHORTS
import org.springframework.stereotype.Repository

@Repository
class MemberCohortRepository(
    private val memberCohortJpaRepository: MemberCohortJpaRepository,
    private val dsl: DSLContext,
) : MemberCohortPersistencePort {
    override fun save(memberCohort: MemberCohort) {
        if (
            existsByMemberIdAndCohortId(
                memberId = memberCohort.memberId.value,
                cohortId = memberCohort.cohortId.value,
            )
        ) {
            return
        }

        dsl
            .insertInto(MEMBER_COHORTS)
            .set(MEMBER_COHORTS.MEMBER_ID, memberCohort.memberId.value)
            .set(MEMBER_COHORTS.COHORT_ID, memberCohort.cohortId.value)
            .execute()
    }

    override fun existsByMemberIdAndCohortId(
        memberId: Long,
        cohortId: Long,
    ): Boolean =
        dsl.fetchExists(
            dsl
                .selectOne()
                .from(MEMBER_COHORTS)
                .where(MEMBER_COHORTS.MEMBER_ID.eq(memberId))
                .and(MEMBER_COHORTS.COHORT_ID.eq(cohortId)),
        )

    override fun deleteByMemberId(memberId: Long) = memberCohortJpaRepository.deleteByMemberId(memberId)
}
