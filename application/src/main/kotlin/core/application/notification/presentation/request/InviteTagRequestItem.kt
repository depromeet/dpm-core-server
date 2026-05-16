package core.application.notification.presentation.request

import core.domain.cohort.vo.AuthorityId
import core.domain.cohort.vo.CohortId
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "알림 대상 태그 식별자 (코호트/권한)")
data class InviteTagRequestItem(
    @Schema(description = "코호트 ID", example = "2")
    val cohortId: CohortId,
    @Schema(description = "권한 ID", example = "2")
    val authorityId: AuthorityId,
)
