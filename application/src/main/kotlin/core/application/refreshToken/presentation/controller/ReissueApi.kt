package core.application.refreshToken.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.refreshToken.presentation.response.TokenResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Tag(name = "Reissue", description = "토큰 재발급 API")
interface ReissueApi {
    @Operation(
        summary = "액세스 토큰 발급 API",
        description = "요청 본문의 refreshToken을 기반으로 액세스 토큰을 재발급합니다.",
        requestBody =
            RequestBody(
                required = true,
                content = [
                    Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = RefreshTokenReissueRequest::class),
                        examples = [
                            ExampleObject(
                                name = "액세스 토큰 발급 요청 예시",
                                value = """
                                    {
                                        "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                                    }
                                """,
                            ),
                        ],
                    ),
                ],
            ),
    )
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
    fun reissue(
        request: RefreshTokenReissueRequest,
        response: HttpServletResponse,
    ): CustomResponse<TokenResponse>
}

data class RefreshTokenReissueRequest(
    val refreshToken: String,
)
