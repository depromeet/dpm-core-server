package com.server.dpmcore.bill.bill.domain.port.inbound.query

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BillMemberIsInvitationSubmittedQueryModel(
    @field:Schema(
        description = "멤버 이름",
        example = "정준원",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val name: String,
    @field:Schema(
        description = "팀 번호",
        example = "6",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val teamNumber: Int,
    @field:Schema(
        description = "권한 이름",
        example = "ORGANIZER",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val authority: String,
    @field:Schema(
        description = "파트",
        example = "SERVER",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val part: String?,
    @field:Schema(
        description = "bill 참여 제출 여부",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val isInvitationSubmitted: Boolean,
)
