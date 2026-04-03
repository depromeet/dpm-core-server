package core.application.notification.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "푸시 토큰 등록 요청")
data class RegisterPushTokenRequest(
    @field:NotBlank(message = "토큰은 필수입니다")
    @Schema(description = "Expo Push Token", example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]", required = true)
    val token: String,
)
