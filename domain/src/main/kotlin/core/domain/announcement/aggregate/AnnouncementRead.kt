package core.domain.announcement.aggregate

import core.domain.announcement.vo.AnnouncementId
import core.domain.announcement.vo.AnnouncementReadId
import core.domain.member.vo.MemberId
import java.time.Instant

class AnnouncementRead(
    val id: AnnouncementReadId? = null,
    val announcementId: AnnouncementId,
    val memberId: MemberId,
    readAt: Instant?,
) {
    var readAt: Instant? = readAt
        private set

    companion object {
        fun create(
            announcementId: AnnouncementId,
            memberId: MemberId,
        ): AnnouncementRead =
            AnnouncementRead(
                announcementId = announcementId,
                memberId = memberId,
                readAt = Instant.now(),
            )

        fun createUnread(
            announcementId: AnnouncementId,
            memberId: MemberId,
        ): AnnouncementRead =
            AnnouncementRead(
                announcementId = announcementId,
                memberId = memberId,
                readAt = null,
            )
    }

    fun isRead(): Boolean = readAt != null
}
