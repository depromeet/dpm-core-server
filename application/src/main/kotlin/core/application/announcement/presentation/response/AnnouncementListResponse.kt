package core.application.announcement.presentation.response

data class AnnouncementListResponse(
    val announcementCount: Int,
    val announcements: List<AnnouncementListItemResponse>,
) {
}
