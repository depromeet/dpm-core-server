package core.domain.member.aggregate

import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId

data class InviteTagSpec(
    val cohortId: CohortId,
    val authorityId: AuthorityId,
    val tagName: String,
) {
    companion object {
        fun of(
            cohortId: CohortId,
            authorityId: AuthorityId,
            tagName: String,
        ): InviteTagSpec =
            InviteTagSpec(
                cohortId = cohortId,
                authorityId = authorityId,
                tagName = tagName,
            )
    }
}
