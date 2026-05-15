package core.application.notification.presentation.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "태그 기반 커스텀 푸시 알림 전송 요청",
    example = """
    {
      "tags": [
        { "cohortId": 1, "authorityId": 1, "tagName": "default" }
      ],
      "title": "알림을 확인해요!",
      "message": "알림 내용도 확인해요"
    }
    """,
)
data class TagBasedNotificationRequest(
    @Schema(description = "알림을 받을 대상 태그 목록")
    val tags: List<TargetTagSpecRequest>,
    @Schema(description = "알림 제목", example = "알림을 확인해요!")
    val title: String,
    @Schema(description = "알림 본문 내용", example = "알림 내용도 확인해요")
    val message: String,
)
