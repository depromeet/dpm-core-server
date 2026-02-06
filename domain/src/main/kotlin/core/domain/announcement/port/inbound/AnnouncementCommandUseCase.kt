package core.domain.announcement.port.inbound

import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.enums.SubmitStatus
import core.domain.announcement.enums.SubmitType
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId
import java.time.Instant

interface AnnouncementCommandUseCase {
    fun create(
        authorId: MemberId,
        announcementType: AnnouncementType,
        submitType: SubmitType,
        title: String,
        content: String?,
        submitLink: String?,
        startAt: Instant,
        dueAt: Instant,
        scheduledAt: Instant?,
        shouldSendNotification: Boolean,
    )

    fun markAsRead(
        memberId: MemberId,
        announcementId: AnnouncementId,
    )

    fun updateSubmitStatus(
        announcementId: AnnouncementId,
        memberIds: List<MemberId>,
        submitStatus: SubmitStatus,
    )
}
