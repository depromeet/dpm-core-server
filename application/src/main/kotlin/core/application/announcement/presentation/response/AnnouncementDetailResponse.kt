package core.application.announcement.presentation.response

import java.time.LocalDateTime

data class AnnouncementDetailResponse(
    val title: String,
    val content: String?,
    val createdAt: LocalDateTime,
    val markAsReadCount: Int,
) {
    companion object {
        fun of(
            title: String,
            content: String?,
            createdAt: LocalDateTime,
            markAsReadCount: Int,
        ): AnnouncementDetailResponse =
            AnnouncementDetailResponse(
                title = title,
                content = content,
                createdAt = createdAt,
                markAsReadCount = markAsReadCount,
            )
    }
}
