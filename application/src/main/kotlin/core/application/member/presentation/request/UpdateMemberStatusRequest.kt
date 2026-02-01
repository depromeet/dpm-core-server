package core.application.member.presentation.request

import core.domain.member.enums.MemberStatus
import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class UpdateMemberStatusRequest(
    @field:NotNull
    @field:Schema(
        description = "멤버 식별자",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val memberId: MemberId,
    @field:NotNull
    @field:Schema(
        description = "멤버 상태",
        example = "PENDING",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val memberStatus: MemberStatus,
)
