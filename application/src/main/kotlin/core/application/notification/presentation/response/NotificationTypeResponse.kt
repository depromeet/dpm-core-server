package core.application.notification.presentation.response

import core.domain.notification.enums.NotificationMessage
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "알림 타입 응답")
data class NotificationTypeResponse(
    @Schema(description = "알림 타입 이름", example = "SESSION_START_SOON")
    val name: String,
    @Schema(description = "알림 제목", example = "세션 시작 30분 전 알림")
    val title: String,
    @Schema(description = "알림 본문 템플릿", example = "{sessionName} 세션이 30분 후 시작됩니다!")
    val bodyTemplate: String,
    @Schema(description = "알림 설명", example = "세션 시작 30분 전 알림")
    val description: String,
) {
    companion object {
        fun from(message: NotificationMessage): NotificationTypeResponse =
            NotificationTypeResponse(
                name = message.name,
                title = message.title,
                bodyTemplate = message.bodyTemplate,
                description = message.description,
            )
    }
}
