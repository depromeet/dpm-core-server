package core.application.member.presentation.request

import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class ConvertDeeperToOrganizerRequest(
    @field:NotNull
    @field:Schema(
        description = "변환 대상 멤버 식별자",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val memberId: MemberId,
)
