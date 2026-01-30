package core.domain.announcement.port.outbound

import core.domain.announcement.aggregate.AnnouncementAssignment

interface AnnouncementAssignmentPersistencePort {
    fun save(announcementAssignment: AnnouncementAssignment): AnnouncementAssignment
}
