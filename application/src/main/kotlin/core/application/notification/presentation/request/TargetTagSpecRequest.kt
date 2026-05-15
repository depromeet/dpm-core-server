package core.application.notification.presentation.request

import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.InviteTagSpec
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "태그 기반 발송 대상 스펙")
data class TargetTagSpecRequest(
    @Schema(description = "코호트 ID", example = "1")
    val cohortId: Long,
    @Schema(description = "권한 ID", example = "1")
    val authorityId: Long,
    @Schema(description = "태그 이름", example = "default")
    val tagName: String,
) {
    fun toInviteTagSpec(): InviteTagSpec =
        InviteTagSpec.of(
            cohortId = CohortId(cohortId),
            authorityId = AuthorityId(authorityId),
            tagName = tagName,
        )
}
