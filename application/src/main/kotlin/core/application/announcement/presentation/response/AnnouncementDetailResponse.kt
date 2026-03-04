package core.application.announcement.presentation.response

import java.time.LocalDateTime

data class AnnouncementDetailResponse(
    val title: String,
    val content: String?,
    val createdAt: LocalDateTime,
    val dueAt: LocalDateTime?,
    val submitLink: String?,
    val isRead: Boolean,
    val markAsReadCount: Int,
) {
    companion object {
        fun of(
            title: String,
            content: String?,
            createdAt: LocalDateTime,
            dueAt: LocalDateTime?,
            submitLink: String?,
            isRead: Boolean,
            markAsReadCount: Int,
        ): AnnouncementDetailResponse =
            AnnouncementDetailResponse(
                title = title,
                content = content,
                createdAt = createdAt,
                dueAt = dueAt,
                submitLink = submitLink,
                isRead = isRead,
                markAsReadCount = markAsReadCount,
            )
    }
}
