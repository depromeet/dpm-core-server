package core.application.member.presentation.request

import jakarta.validation.constraints.NotBlank

data class AppleMemberProfileUpdateRequest(
    @field:NotBlank(message = "name은 필수입니다")
    val name: String,
    @field:NotBlank(message = "part는 필수입니다")
    val part: String,
)
