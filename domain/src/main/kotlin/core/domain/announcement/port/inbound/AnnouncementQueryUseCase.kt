package core.domain.announcement.port.inbound

import core.domain.announcement.aggregate.Announcement
import core.domain.announcement.aggregate.AnnouncementRead
import core.domain.announcement.aggregate.AssignmentSubmission
import core.domain.announcement.vo.AnnouncementId
import core.domain.announcement.vo.AssignmentId

interface AnnouncementQueryUseCase {
    fun getAnnouncementById(announcementId: AnnouncementId): Announcement

    fun findAnnouncementByAssignmentId(assignmentId: AssignmentId): Announcement?

    fun getAll(): List<Announcement>

    fun findUnreadByAnnouncementId(announcementId: AnnouncementId): List<AnnouncementRead>

    fun findUnsubmittedByAnnouncementIdAndSubmitStatus(announcementId: AnnouncementId): List<AssignmentSubmission>
}
