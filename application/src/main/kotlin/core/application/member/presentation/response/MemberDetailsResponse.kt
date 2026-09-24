package core.application.member.presentation.response

import core.domain.member.aggregate.Member
import core.domain.team.vo.TeamNumber
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

data class MemberDetailsResponse(
    @field:Schema(
        description = "이메일",
        example = "depromeetcore@gmail.com",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val email: String,
    @field:Schema(
        description = "이름",
        example = "디프만",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val name: String?,
    @field:Schema(
        description = "파트",
        example = "WEB",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    val part: String?,
    @field:Schema(
        description = "기수",
        example = "17",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val cohort: String?,
    @field:Schema(
        description = "팀 번호",
        example = "3",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    val teamNumber: TeamNumber,
    @field:Schema(
        description = "어드민 여부",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val isAdmin: Boolean,
    @field:Schema(
        description = "멤버 상태",
        example = "ACTIVE",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val status: String,
    @field:ArraySchema(
        arraySchema =
            Schema(
                description = "연동된 로그인 수단 목록",
                requiredMode = Schema.RequiredMode.REQUIRED,
            ),
    )
    val loginMethods: List<LoginMethod>,
) {
    data class LoginMethod(
        @field:Schema(
            description = "로그인 수단 (소셜 제공자 및 이메일/비밀번호)",
            example = "KAKAO",
            allowableValues = ["KAKAO", "APPLE", "EMAIL"],
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val type: String,
        @field:Schema(
            description = "해당 로그인 수단의 이메일. 이메일 저장 이전에 연동되어 아직 재로그인하지 않은 경우 null",
            example = "depromeetcore@gmail.com",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true,
        )
        val email: String?,
    )

    companion object {
        fun of(
            member: Member,
            isAdmin: Boolean,
            teamNumber: TeamNumber,
            loginMethods: List<LoginMethod>,
        ): MemberDetailsResponse =
            MemberDetailsResponse(
                email = member.signupEmail,
                name = member.name,
                part = member.part?.name,
                cohort = member.latestCohortValue(),
                teamNumber = teamNumber,
                isAdmin = isAdmin,
                status = member.status.name,
                loginMethods = loginMethods,
            )
    }
}
