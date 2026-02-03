package core.application.announcement.presentation.response

data class AnnouncementViewMemberListResponse(
    val readMembers: List<AnnouncementViewMemberListItemResponse>,
    val unreadMembers: List<AnnouncementViewMemberListItemResponse>,
)
