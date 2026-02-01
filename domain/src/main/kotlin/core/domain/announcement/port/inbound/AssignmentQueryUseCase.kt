package core.domain.announcement.port.inbound

import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.vo.AnnouncementId

interface AssignmentQueryUseCase {
    fun getAllAssignments(): List<Assignment>

    fun getAssignmentByAnnouncementId(announcementId: AnnouncementId): Assignment
}
