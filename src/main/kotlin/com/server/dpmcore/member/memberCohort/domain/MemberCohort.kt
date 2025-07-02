package com.server.dpmcore.member.memberCohort.domain

import com.server.dpmcore.cohort.domain.CohortId
import com.server.dpmcore.member.member.domain.MemberId

data class MemberCohort(
    val id: MemberCohortId,
    val memberId: MemberId,
    val cohortId: CohortId,
)
