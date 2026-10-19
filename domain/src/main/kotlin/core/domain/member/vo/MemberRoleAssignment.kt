package core.domain.member.vo

import core.domain.cohort.vo.CohortId

data class MemberRoleAssignment(
    val roleName: String,
    val cohortId: CohortId?,
)
