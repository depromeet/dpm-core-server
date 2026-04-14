package core.application.member.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Member-Login", description = "Member Login API")
@RestController
@RequestMapping("/login")
class MemberLoginController {
    companion object {
        private const val KAKAO_AUTHORIZATION_PATH = "/oauth2/authorization/kakao"
        private const val APPLE_AUTHORIZATION_PATH = "/oauth2/authorization/apple"
    }

    @GetMapping("/kakao")
    @Operation(
        summary = "Kakao OAuth2 Authorization Path",
        description = "Returns the relative path that starts the Kakao OAuth2 authorization flow.",
    )
    fun login(): OAuthAuthorizationPathResponse = OAuthAuthorizationPathResponse(KAKAO_AUTHORIZATION_PATH)

    @Operation(
        summary = "Apple OAuth2 Authorization Path",
        description = "Returns the relative path that starts the Apple OAuth2 authorization flow.",
    )
    @GetMapping("/apple")
    fun appleLogin(): OAuthAuthorizationPathResponse = OAuthAuthorizationPathResponse(APPLE_AUTHORIZATION_PATH)

    data class OAuthAuthorizationPathResponse(
        val authorizationPath: String,
    )

    data class AppleLoginRequest(
        val authorizationCode: String,
        val fullName: String? = null,
        val familyName: String? = null,
        val givenName: String? = null,
    )
}
