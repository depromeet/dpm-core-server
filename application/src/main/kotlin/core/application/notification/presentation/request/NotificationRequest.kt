package core.application.notification.presentation.request

import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "커스텀 푸시 알림 전송 요청",
    example = """
    {
      "targetMemberId": 13,
      "title": "알림을 확인해요!",
      "message": "알림 내용도 확인해요"
    }
    """,
)
data class NotificationRequest(
    @Schema(description = "알림을 받을 회원 ID", example = "13")
    val targetMemberId: MemberId,
    @Schema(description = "알림 제목", example = "알림을 확인해요!")
    val title: String,
    @Schema(description = "알림 본문 내용", example = "알림 내용도 확인해요")
    val message: String,
)
