package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.application.service.auth.KakaoLoginTokenSaveService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Member-Login", description = "Member Login API")
@RestController
class MemberKakaoTokenController(
    private val kakaoLoginTokenSaveService: KakaoLoginTokenSaveService,
) {
    @PostMapping("/login/auth/kakao/tokens")
    @Operation(
        summary = "Kakao Login Token Save",
        description =
            "Receives backend-issued JWT accessToken/refreshToken in" +
                " the request body and stores them as backend cookies." +
                "This endpoint does not accept Kakao OAuth provider tokens.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Token cookies saved successfully"),
            ApiResponse(responseCode = "400", description = "Invalid token"),
            ApiResponse(responseCode = "500", description = "Internal server error"),
        ],
    )
    fun saveKakaoLoginTokens(
        @RequestBody @Valid request: KakaoLoginTokenSaveRequest,
        response: HttpServletResponse,
    ): CustomResponse<Void> {
        kakaoLoginTokenSaveService.save(request.accessToken, request.refreshToken, response)
        return CustomResponse.ok()
    }

    data class KakaoLoginTokenSaveRequest(
        @field:NotBlank(message = "accessToken은 필수입니다")
        val accessToken: String,
        @field:NotBlank(message = "refreshToken은 필수입니다")
        val refreshToken: String,
    )
}
