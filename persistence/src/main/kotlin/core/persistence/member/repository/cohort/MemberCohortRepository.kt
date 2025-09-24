package core.persistence.member.repository.cohort

import core.domain.member.aggregate.MemberCohort
import core.domain.member.port.outbound.MemberCohortPersistencePort
import jooq.dsl.tables.references.MEMBER_COHORTS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class MemberCohortRepository(
    private val memberCohortJpaRepository: MemberCohortJpaRepository,
    private val dsl: DSLContext,
) : MemberCohortPersistencePort {
    override fun save(memberCohort: MemberCohort) {
        dsl
            .insertInto(MEMBER_COHORTS)
            .set(MEMBER_COHORTS.MEMBER_ID, memberCohort.memberId.value)
            .set(MEMBER_COHORTS.COHORT_ID, memberCohort.cohortId.value)
            .execute()
    }

    override fun deleteByMemberId(memberId: Long) = memberCohortJpaRepository.deleteByMemberId(memberId)
}
