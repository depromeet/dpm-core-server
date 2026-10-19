package core.domain.announcement.port.outbound

import core.domain.announcement.aggregate.Announcement
import core.domain.announcement.port.outbound.query.AnnouncementListItemQueryModel
import core.domain.announcement.vo.AnnouncementId
import core.domain.announcement.vo.AssignmentId
import core.domain.cohort.vo.CohortId

interface AnnouncementPersistencePort {
    fun save(announcement: Announcement): Announcement

    fun findByCohortId(cohortId: CohortId): List<Announcement>

    fun findAnnouncementListItems(cohortId: CohortId): List<AnnouncementListItemQueryModel>

    fun findAnnouncementById(announcementId: AnnouncementId): Announcement?

    fun softDeleteByAnnouncement(announcement: Announcement): Announcement

    fun update(announcement: Announcement): Announcement

    fun findByAssignmentId(assignmentId: AssignmentId): Announcement?
}
