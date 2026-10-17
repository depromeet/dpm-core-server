package core.application.authorization.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class UpdateMemberRoleRequest(
    @field:NotBlank
    @field:Pattern(regexp = "MASTER|CORE|ORGANIZER|DEEPER|GUEST", message = "roleType must be one of MASTER, CORE, ORGANIZER, DEEPER, GUEST")
    @field:Schema(description = "역할 타입", example = "ORGANIZER", requiredMode = Schema.RequiredMode.REQUIRED)
    val roleType: String,
    @field:NotBlank
    @field:Pattern(regexp = "\\d+기?", message = "cohort must be numeric (e.g. 18 or 18기)")
    @field:Schema(description = "역할이 적용될 기수 값", example = "18", requiredMode = Schema.RequiredMode.REQUIRED)
    val cohort: String,
)
