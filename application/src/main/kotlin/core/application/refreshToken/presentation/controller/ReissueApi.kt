package core.application.refreshToken.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.refreshToken.presentation.response.TokenResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Tag(name = "Reissue", description = "토큰 재발급 API")
interface ReissueApi {
    @ApiResponse(
        responseCode = "200",
        description = "토큰 재발급 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "토큰 재발급 성공 응답",
                        value = """
                            {
                                "status": "OK",
                                "code": "G000",
                                "message": "요청에 성공했습니다",
                                "data": {
                                    "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiI0MjAxOTcyNzc",
                                    "expirationTime": 3600
                                }
                            }
                        """,
                    ),
                ],
            ),
        ],
    )
    @Operation(summary = "액세스 토큰 발급 API", description = "쿠키의 리프레시 토큰을 추출하여 액세스 토큰을 발급합니다.")
    fun reissue(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): CustomResponse<TokenResponse>
}
