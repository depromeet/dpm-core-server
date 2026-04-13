package core.domain.announcement.event

import core.domain.announcement.aggregate.Announcement
import core.domain.member.vo.MemberId

data class AnnouncementRemindToMembersEvent(
    val announcement: Announcement,
    val memberIds: List<MemberId>,
) {
    companion object {
        fun of(
            announcement: Announcement,
            memberIds: List<MemberId>,
        ): AnnouncementRemindToMembersEvent =
            AnnouncementRemindToMembersEvent(
                announcement = announcement,
                memberIds = memberIds,
            )
    }
}
