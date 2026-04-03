package core.application.notification.presentation.response

import core.application.notification.application.exception.NotificationTokenNotFoundException
import core.domain.member.vo.MemberId
import core.domain.notification.aggregate.NotificationToken
import core.domain.notification.vo.NotificationTokenId
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "푸시 토큰 응답")
data class NotificationTokenResponse(
    @Schema(description = "토큰 ID", example = "1")
    val id: NotificationTokenId,
    @Schema(description = "멤버 ID", example = "13")
    val memberId: MemberId,
    @Schema(description = "Expo Push Token", example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]")
    val token: String,
    @Schema(description = "생성일시")
    val createdAt: Instant?,
    @Schema(description = "수정일시")
    val updatedAt: Instant?,
) {
    companion object {
        fun from(notificationToken: NotificationToken): NotificationTokenResponse =
            NotificationTokenResponse(
                id = notificationToken.id ?: throw NotificationTokenNotFoundException(),
                memberId = notificationToken.memberId,
                token = notificationToken.token,
                createdAt = notificationToken.createdAt,
                updatedAt = notificationToken.updatedAt,
            )
    }
}
