package core.domain.announcement.port.inbound

import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.vo.AnnouncementId
import core.domain.cohort.vo.CohortId
import java.time.Instant

interface AssignmentQueryUseCase {
    fun getAllAssignments(): List<Assignment>

    fun getAssignmentsByCohortId(cohortId: CohortId): List<Assignment>

    fun getAssignmentByAnnouncementId(announcementId: AnnouncementId): Assignment

    fun findAssignmentByAnnouncementId(announcementId: AnnouncementId): Assignment?

    fun findByDueAtBetween(
        start: Instant,
        end: Instant,
    ): List<Assignment>
}
