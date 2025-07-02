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
        if (other !is MemberCohort) return false

        return id == other.id &&
            memberId == other.memberId &&
            cohortId == other.cohortId
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + memberId.hashCode()
        result = 31 * result + cohortId.hashCode()
        return result
    }
}
