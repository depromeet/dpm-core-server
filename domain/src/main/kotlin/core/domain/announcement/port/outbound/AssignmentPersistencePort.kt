package core.domain.announcement.port.outbound

import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.vo.AnnouncementId

interface AssignmentPersistencePort {
    fun save(assignment: Assignment): Assignment

    fun findAll(): List<Assignment>

    fun findByAnnouncementId(announcementId: AnnouncementId): Assignment
}
