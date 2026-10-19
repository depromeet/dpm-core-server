package core.application.member

import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberCohort
import core.domain.member.enums.MemberStatus
import core.domain.member.vo.MemberCohortId
import core.domain.member.vo.MemberId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MemberLatestCohortTest {
    @Test
    fun pastCohortAddedLaterDoesNotBecomeLatest() {
        // 18기(id=115) 소속 이후 17기(id=118)가 나중에 추가된 경우
        val member = member(cohort(id = 115L, cohortId = 2L), cohort(id = 118L, cohortId = 1L))

        assertEquals(CohortId(2L), member.latestCohortId())
    }

    @Test
    fun duplicatedCohortRowsPreferLatestInserted() {
        val member = member(cohort(id = 10L, cohortId = 2L), cohort(id = 20L, cohortId = 2L), cohort(id = 30L, cohortId = 1L))

        assertEquals(MemberCohortId(20L), member.latestMemberCohort()?.id)
    }

    @Test
    fun noCohortReturnsNull() {
        assertNull(member().latestCohortId())
    }

    private fun member(vararg cohorts: MemberCohort) =
        Member(
            id = MemberId(1L),
            name = "테스트",
            signupEmail = "test@example.com",
            status = MemberStatus.ACTIVE,
            memberCohorts = cohorts.toList(),
        )

    private fun cohort(
        id: Long,
        cohortId: Long,
    ) = MemberCohort(
        id = MemberCohortId(id),
        memberId = MemberId(1L),
        cohortId = CohortId(cohortId),
    )
}
