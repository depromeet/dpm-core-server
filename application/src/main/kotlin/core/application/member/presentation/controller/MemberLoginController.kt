package core.application.member.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "Member-Login", description = "Member Login API")
@Controller
@RequestMapping("/login")
class MemberLoginController {
    companion object {
        private const val KAKAO_REDIRECT_URL = "redirect:/oauth2/authorization/kakao"
        private const val APPLE_REDIRECT_URL = "redirect:/oauth2/authorization/apple"
    }

    @GetMapping("/kakao")
    @Operation(
        summary = "Kakao OAuth2 Login Redirect",
        description = "Initiates Kakao OAuth2 authorization flow.",
    )
    fun login(): String = KAKAO_REDIRECT_URL

    @Operation(
        summary = "Apple OAuth2 Login Redirect",
        description = "Initiates Apple OAuth2 authorization flow.",
    )
    @GetMapping("/apple")
    fun appleLogin(): String = APPLE_REDIRECT_URL

    data class AppleLoginRequest(
        val authorizationCode: String,
        val fullName: String? = null,
        val familyName: String? = null,
        val givenName: String? = null,
    )
}
