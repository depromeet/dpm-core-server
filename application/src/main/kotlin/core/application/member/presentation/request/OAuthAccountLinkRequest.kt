package core.application.member.presentation.request

import jakarta.validation.constraints.NotBlank

data class OAuthAccountLinkRequest(
    @field:NotBlank(message = "authorizationCode는 필수입니다")
    val authorizationCode: String,
)
