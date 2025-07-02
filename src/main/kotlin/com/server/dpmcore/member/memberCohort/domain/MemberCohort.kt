package com.server.dpmcore.member.memberCohort.domain

import com.server.dpmcore.cohort.domain.model.CohortId
import com.server.dpmcore.member.member.domain.model.MemberId

data class MemberCohort(
    val id: MemberCohortId? = null,
    val memberId: MemberId,
    val cohortId: CohortId,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemberCohort

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
