package core.application.coreDev.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

data class CohortInfoDetailResponse(
    @field:Schema(
        description = "기수",
        example = "17",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val cohort: String,
    @field:Schema(
        description = "팀 번호",
        example = "3",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    val teamNumber: Int,
    @field:Schema(
        description = "어드민 여부",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val isAdmin: Boolean,
) {
    companion object {
        fun of(
            cohort: String,
            teamNumber: Int,
            authorities: List<String>,
        ): CohortInfoDetailResponse =
            CohortInfoDetailResponse(
                cohort = cohort,
                teamNumber = teamNumber,
                isAdmin = authorities.any { it == ADMIN_AUTHORITY },
            )

        private const val ADMIN_AUTHORITY = "ORGANIZER"
    }
}
