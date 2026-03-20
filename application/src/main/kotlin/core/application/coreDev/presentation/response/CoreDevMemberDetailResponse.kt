package core.application.coreDev.presentation.response

import core.domain.member.aggregate.Member
import io.swagger.v3.oas.annotations.media.Schema

data class CoreDevMemberDetailResponse(
    @field:Schema(
        description = "이메일",
        example = "depromeetcore@gmail.com",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val email: String,
    @field:Schema(
        description = "이름",
        example = "디프만 코어",
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
    val cohortInfos: List<CohortInfoDetailResponse>,
    @field:Schema(
        description = "멤버 상태",
        example = "ACTIVE",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val status: String,
) {
    companion object {
        fun of(
            member: Member,
            cohortInfos: List<CohortInfoDetailResponse>,
        ): CoreDevMemberDetailResponse =
            CoreDevMemberDetailResponse(
                email = member.signupEmail,
                name = member.name,
                part = member.part?.name,
                cohortInfos = cohortInfos,
                status = member.status.name,
            )
    }
}
