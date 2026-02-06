package core.domain.gathering.enums

import core.domain.cohort.vo.CohortId

enum class GatheringV2InviteTag(
    val cohortId: CohortId,
    val authorityId: Long,
    val tagName: String,
) {
    DEEPER_17TH(CohortId(1L), 1L, "17기 디퍼"),
    ORGANIZER_17TH(CohortId(1L), 2L, "17기 운영진"),
    DEEPER_18TH(CohortId(2L), 1L, "18기 디퍼"),
    ORGANIZER_18TH(CohortId(2L), 2L, "18기 운영진"),
    ;

    companion object {
        fun from(
            cohortId: CohortId,
            authorityId: Long,
        ): GatheringV2InviteTag =
            GatheringV2InviteTag.entries.find {
                it.cohortId == cohortId && it.authorityId == authorityId
            } ?: throw IllegalArgumentException(
                "일치하는 태그를 찾을 수 없습니다: cohortId=${cohortId.value}, authorityId=$authorityId",
            )
    }
}
