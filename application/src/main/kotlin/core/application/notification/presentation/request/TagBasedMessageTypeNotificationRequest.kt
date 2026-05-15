package core.application.notification.presentation.request

import core.domain.notification.enums.NotificationMessageType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "태그 기반 메시지 타입 푸시 알림 전송 요청",
    example = """
    {
      "tags": [
        { "cohortId": 1, "authorityId": 1, "tagName": "default" }
      ],
      "notificationMessageType": "SESSION_START_SOON",
      "variables": {
        "title": "테스트를 위한 타이틀입니다"
      }
    }
    """,
)
data class TagBasedMessageTypeNotificationRequest(
    @Schema(description = "알림을 받을 대상 태그 목록")
    val tags: List<TargetTagSpecRequest>,
    @Schema(description = "알림 메시지 타입")
    val notificationMessageType: NotificationMessageType,
    @Schema(description = "메시지 템플릿 변수")
    val variables: Map<String, String> = emptyMap(),
)
