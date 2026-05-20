package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.application.service.auth.KakaoLoginTokenSaveService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Member-Login", description = "Member Login API")
@RestController
class MemberKakaoTokenController(
    private val kakaoLoginTokenSaveService: KakaoLoginTokenSaveService,
) {
    @PostMapping("/login/auth/kakao/tokens")
    @Operation(
        summary = "Kakao Login Token Save",
        description = "Receives Kakao login tokens as request parameters and stores them as backend cookies.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Token cookies saved successfully"),
            ApiResponse(responseCode = "400", description = "Invalid token"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    fun saveKakaoLoginTokens(
        @RequestParam accessToken: String,
        @RequestParam refreshToken: String,
        response: HttpServletResponse,
    ): CustomResponse<Void> {
        kakaoLoginTokenSaveService.save(accessToken, refreshToken, response)
        return CustomResponse.ok()
    }
}
