package core.application.member.presentation.response

import core.domain.member.aggregate.Member
import core.domain.member.enums.LoginMethod
import core.domain.team.vo.TeamNumber
import io.swagger.v3.oas.annotations.media.Schema

data class MemberDetailsResponse(
    @field:Schema(
        description = "현재 세션의 로그인 수단에 해당하는 이메일. 로그인 수단을 알 수 없거나 저장된 이메일이 없으면 가입 이메일",
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
    @field:Schema(
        description = "현재 세션의 로그인 수단. 로그인 수단이 기록되기 전에 발급된 토큰이면 null",
        example = "KAKAO",
        allowableValues = ["KAKAO", "APPLE", "EMAIL"],
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true,
    )
    val loginMethod: String?,
) {
    companion object {
        fun of(
            member: Member,
            email: String,
            isAdmin: Boolean,
            teamNumber: TeamNumber,
            loginMethod: LoginMethod?,
        ): MemberDetailsResponse =
            MemberDetailsResponse(
                email = email,
                name = member.name,
                part = member.part?.name,
                cohort = member.latestCohortValue(),
                teamNumber = teamNumber,
                isAdmin = isAdmin,
                status = member.status.name,
                loginMethod = loginMethod?.name,
            )
    }
}
