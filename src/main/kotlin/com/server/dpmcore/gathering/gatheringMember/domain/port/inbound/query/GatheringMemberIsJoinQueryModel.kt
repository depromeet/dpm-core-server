package com.server.dpmcore.gathering.gatheringMember.domain.port.inbound.query

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GatheringMemberIsJoinQueryModel(
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
        description = "gathering 참여 여부",
        example = "true",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = false,
    )
    val isJoined: Boolean?,
) {
    companion object {
        fun of(
            name: String,
            teamNumber: Int,
            authority: String,
            part: String?,
            isJoined: Boolean?,
        ) = GatheringMemberIsJoinQueryModel(
            name = name,
            teamNumber = teamNumber,
            authority = authority,
            part = part,
            isJoined = isJoined,
        )
    }
}
