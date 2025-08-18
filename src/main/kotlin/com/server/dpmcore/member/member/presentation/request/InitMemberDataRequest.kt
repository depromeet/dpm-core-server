package com.server.dpmcore.member.member.presentation.request

import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.domain.model.MemberPart
import com.server.dpmcore.team.domain.model.TeamId
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class InitMemberDataRequest(
    @field:NotNull
    @field:Schema(
        description = "팀 식별자",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val teamId: TeamId,
    @field:Schema(
        description = "멤버 목록",
        example = """
            [
                {
                    "memberId": 1,
                    "memberPart": "SERVER"
                },
                {
                    "memberId": 2,
                    "memberPart": "WEB"
                }
            ]
        """,
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val members: List<MemberData>,
) {
    data class MemberData(
        @field:NotNull
        @field:Schema(
            description = "멤버 식별자",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val memberId: MemberId,
        @field:NotNull
        @field:Schema(
            description = "멤버 파트",
            example = "SERVER",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val memberPart: MemberPart,
    )
}
