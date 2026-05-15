package core.application.notification.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "태그 기반 푸시 알림 발송 응답")
data class TagBasedNotificationResponse(
    @Schema(description = "발송 대상 멤버 수", example = "42")
    val targetMemberCount: Int,
)
