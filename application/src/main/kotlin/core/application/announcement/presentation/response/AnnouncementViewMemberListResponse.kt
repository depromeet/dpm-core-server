package core.application.announcement.presentation.response

data class AnnouncementViewMemberListResponse(
    val readMembers: List<AnnouncementViewMemberListItemResponse>,
    val unreadMembers: List<AnnouncementViewMemberListItemResponse>,
) {
    companion object {
        fun of(
            readMembers: List<AnnouncementViewMemberListItemResponse>,
            unreadMembers: List<AnnouncementViewMemberListItemResponse>,
        ): AnnouncementViewMemberListResponse =
            AnnouncementViewMemberListResponse(
                readMembers = readMembers,
                unreadMembers = unreadMembers,
            )
    }
}
