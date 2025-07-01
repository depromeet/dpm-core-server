package com.server.dpmcore.member.memberCohort.domain

data class MemberCohort(
    val id: MemberCohortId,
    val memberId: String,
    val cohortId: String,
)
