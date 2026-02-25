package core.domain.gathering.aggregate

import core.domain.cohort.vo.CohortId
import core.domain.gathering.vo.GatheringV2Id
import core.domain.gathering.vo.GatheringV2InviteTagId
import java.time.Instant

class GatheringV2InviteTag(
    val id: GatheringV2InviteTagId? = null,
    val gatheringId: GatheringV2Id,
    val cohortId: CohortId,
    val authorityId: Long,
    val tagName: String,
    val createdAt: Instant? = null,
) {
    companion object {
        fun create(
            gatheringId: GatheringV2Id,
            cohortId: CohortId,
            authorityId: Long,
            tagName: String,
        ): GatheringV2InviteTag =
            GatheringV2InviteTag(
                gatheringId = gatheringId,
                cohortId = cohortId,
                authorityId = authorityId,
                tagName = tagName,
                createdAt = Instant.now(),
            )
    }
}
