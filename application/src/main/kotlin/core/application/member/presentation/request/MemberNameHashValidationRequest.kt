package core.application.member.presentation.request

import jakarta.validation.constraints.NotBlank

data class MemberNameHashValidationRequest(
    @field:NotBlank(message = "name은 필수입니다")
    val name: String,
)
