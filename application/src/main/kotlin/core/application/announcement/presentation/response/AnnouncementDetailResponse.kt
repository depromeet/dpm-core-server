package core.application.announcement.presentation.response

import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.vo.AnnouncementId
import java.time.LocalDateTime

data class AnnouncementDetailResponse(
    val announcementId: AnnouncementId,
    val announcementType: AnnouncementType,
    val title: String,
    val content: String?,
    val createdAt: LocalDateTime,
    val assignment: AnnouncementDetailAssignmentResponse?,
    val isRead: Boolean,
    val markAsReadCount: Int,
) {
    companion object {
        fun of(
            announcementId: AnnouncementId,
            announcementType: AnnouncementType,
            title: String,
            content: String?,
            createdAt: LocalDateTime,
            announcementDetailAssignmentResponse: AnnouncementDetailAssignmentResponse? = null,
            isRead: Boolean,
            markAsReadCount: Int,
        ): AnnouncementDetailResponse =
            AnnouncementDetailResponse(
                announcementId = announcementId,
                announcementType = announcementType,
                title = title,
                content = content,
                createdAt = createdAt,
                assignment = announcementDetailAssignmentResponse,
                isRead = isRead,
                markAsReadCount = markAsReadCount,
            )
    }
}
