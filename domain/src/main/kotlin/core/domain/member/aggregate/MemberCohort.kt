package core.domain.member.aggregate

import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberCohortId
import core.domain.member.vo.MemberId

class MemberCohort(
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

    override fun toString(): String = "MemberCohort(id=$id, memberId=$memberId, cohortId=$cohortId)"

    companion object {
        fun of(
            memberId: MemberId,
            cohortId: CohortId,
        ): MemberCohort =
            MemberCohort(
                memberId = memberId,
                cohortId = cohortId,
            )
    }
}
