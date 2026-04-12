package core.application.authorization.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

data class UpdateMemberRoleRequest(
    @field:NotNull
    @field:Schema(
        description = "운영진 여부. true면 해당 기수 운영진, false면 해당 기수 디퍼로 변경합니다.",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val isAdmin: Boolean,
    @field:NotBlank
    @field:Pattern(regexp = "\\d+기?", message = "cohort must be numeric (e.g. 17 or 17기)")
    @field:Schema(
        description = "역할을 변경할 기수 값. 17 또는 17기 형식을 허용합니다.",
        example = "17",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val cohort: String,
)
