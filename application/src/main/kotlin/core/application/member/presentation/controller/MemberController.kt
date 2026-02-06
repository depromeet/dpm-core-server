package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.application.service.MemberCommandService
import core.application.member.application.service.MemberQueryService
import core.application.member.application.service.auth.AppleAuthService
import core.application.member.application.service.auth.AuthTokenResponse
import core.application.member.presentation.controller.MemberLoginController.AppleLoginRequest
import core.application.member.presentation.request.InitMemberDataRequest
import core.application.member.presentation.request.UpdateMemberStatusRequest
import core.application.member.presentation.request.WhiteListCheckRequest
import core.application.member.presentation.response.MemberDetailsResponse
import core.application.security.properties.SecurityProperties
import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/members")
class MemberController(
    private val memberQueryService: MemberQueryService,
    private val memberCommandService: MemberCommandService,
    private val appleAuthService: AppleAuthService,
    private val securityProperties: SecurityProperties,
    ) : MemberApi {
    //    @PreAuthorize("hasAuthority('read:member')")
    @PreAuthorize("permitAll()")
    @GetMapping("/me")
    override fun me(memberId: MemberId): CustomResponse<MemberDetailsResponse> {
        val response: MemberDetailsResponse = memberQueryService.memberMe(memberId)
        return CustomResponse.ok(response)
    }

    //    @PreAuthorize("hasAuthority('delete:member')")
    @PreAuthorize("permitAll()")
    @PatchMapping("/withdraw")
    override fun withdraw(
        memberId: MemberId,
        response: HttpServletResponse,
    ): CustomResponse<Void> {
        memberCommandService.withdraw(memberId, response)
        return CustomResponse.ok()
    }

    //    @PreAuthorize("hasAuthority('create:member')")
    @PreAuthorize("permitAll()")
    @PatchMapping("/init")
    override fun initMemberDataAndApprove(
        @Valid @RequestBody request: InitMemberDataRequest,
    ): CustomResponse<Void> {
        memberCommandService.initMemberDataAndApprove(request)
        return CustomResponse.ok()
    }

    //    @PreAuthorize("hasAuthority('create:member')")
    @PreAuthorize("permitAll()")
    @PatchMapping("/whitelist")
    override fun checkWhiteList(
        @Valid @RequestBody request: WhiteListCheckRequest,
    ): CustomResponse<Void> {
        memberQueryService
            .checkWhiteList(request.name, request.signupEmail)
            ?.let { member -> memberCommandService.activate(member) }
        // If null, OAuth user is already activated - ignore gracefully

        return CustomResponse.ok()
    }

    // @PreAuthorize("hasAuthority('create:member')")  // Temporarily disabled for testing
    @PatchMapping("/status")
    override fun updateMemberStatus(
        @Valid @RequestBody request: UpdateMemberStatusRequest,
    ): CustomResponse<Void> {
        memberCommandService.updateMemberStatus(request)
        return CustomResponse.ok()
    }

    @PostMapping("/login/auth/apple")
    @Operation(
        summary = "Apple OAuth2 Login V1",
        description = "Login with Apple authorization code to receive JWT tokens",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Login successful - returns JWT tokens"),
            ApiResponse(responseCode = "401", description = "Invalid authorization code"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    fun appleLoginV1(
        @RequestBody body: AppleLoginRequest,
        response: HttpServletResponse,
    ): AuthTokenResponse {
        val tokens = appleAuthService.login(body.authorizationCode)
        addTokenCookies(response, tokens)
        return tokens
    }
    private fun addTokenCookies(
        response: HttpServletResponse,
        tokens: AuthTokenResponse,
    ) {
        val accessTokenCookie = createCookie("accessToken", tokens.accessToken, 60 * 60 * 24) // 1 day
        val refreshTokenCookie = createCookie("refreshToken", tokens.refreshToken, 60 * 60 * 24 * 30) // 30 days

        response.addCookie(accessTokenCookie)
        response.addCookie(refreshTokenCookie)
    }
    private fun createCookie(
        name: String,
        value: String,
        maxAgeSeconds: Int,
    ): Cookie {
        return Cookie(name, value).apply {
            path = "/"
            domain =
                if (securityProperties.cookie.domain != "localhost") {
                    securityProperties.cookie.domain
                } else {
                    null
                }
            maxAge = maxAgeSeconds
            isHttpOnly = true
            secure = securityProperties.cookie.secure // Use config value (true for dev/prod, false for local)
            setAttribute("SameSite", "None")
        }
    }
}
