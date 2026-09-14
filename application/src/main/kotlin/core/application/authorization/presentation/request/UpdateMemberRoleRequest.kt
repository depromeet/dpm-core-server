package core.application.authorization.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

data class UpdateMemberRoleRequest(
    @field:NotBlank
    @field:Pattern(regexp = "MASTER|CORE|ORGANIZER|DEEPER|GUEST", message = "roleType must be one of MASTER, CORE, ORGANIZER, DEEPER, GUEST")
    @field:Schema(description = "역할 타입", example = "ORGANIZER", requiredMode = Schema.RequiredMode.REQUIRED)
    val roleType: String,
    @field:NotNull
    @field:Schema(description = "역할이 적용될 기수 ID", example = "18", requiredMode = Schema.RequiredMode.REQUIRED)
    val cohortId: Long,
)
