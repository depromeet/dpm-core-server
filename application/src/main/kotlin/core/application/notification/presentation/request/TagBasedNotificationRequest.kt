package core.application.notification.presentation.request

import core.domain.member.aggregate.InviteTagSpec
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "태그 기반 커스텀 푸시 알림 전송 요청",
    example = """
    {
      "tags": [
        { "cohortId": 2, "authorityId": 2 }
      ],
      "title": "알림을 확인해요!",
      "message": "알림 내용도 확인해요"
    }
    """,
)
data class TagBasedNotificationRequest(
    @Schema(description = "알림을 받을 대상 태그 목록")
    val tags: List<InviteTagRequestItem>,
    @Schema(description = "알림 제목", example = "알림을 확인해요!")
    val title: String,
    @Schema(description = "알림 본문 내용", example = "알림 내용도 확인해요")
    val message: String,
) {
    fun toInviteTagSpecs(): List<InviteTagSpec> = this.tags.map { InviteTagSpec.of(it.cohortId, it.authorityId) }
}
