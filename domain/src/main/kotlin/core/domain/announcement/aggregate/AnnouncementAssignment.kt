package core.domain.announcement.aggregate

import core.domain.announcement.vo.AnnouncementAssignmentId
import core.domain.announcement.vo.AnnouncementId
import core.domain.announcement.vo.AssignmentId

class AnnouncementAssignment(
    val id: AnnouncementAssignmentId? = null,
    val assignmentId: AssignmentId,
    val announcementId: AnnouncementId,
) {
    companion object {
        fun create(
            assignmentId: AssignmentId,
            announcementId: AnnouncementId,
        ): AnnouncementAssignment {
            return AnnouncementAssignment(
                assignmentId = assignmentId,
                announcementId = announcementId,
            )
        }
    }
}
