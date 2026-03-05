package core.domain.announcement.port.inbound

import core.domain.announcement.aggregate.AnnouncementAssignment
import core.domain.announcement.aggregate.Assignment

interface AnnouncementAssignmentCommandUseCase {
    fun create(
        announcementAssignment: AnnouncementAssignment,
        assignment: Assignment,
    ): AnnouncementAssignment
}
