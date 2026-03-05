package core.application.announcement.application.service

import core.domain.announcement.aggregate.AnnouncementAssignment
import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.port.inbound.AnnouncementAssignmentCommandUseCase
import core.domain.announcement.port.inbound.AssignmentSubmissionCommandUseCase
import core.domain.announcement.port.outbound.AnnouncementAssignmentPersistencePort
import org.springframework.stereotype.Service

@Service
class AnnouncementAssignmentCommandService(
    val announcementAssignmentPersistencePort: AnnouncementAssignmentPersistencePort,
    val assignmentSubmissionCommandUseCase: AssignmentSubmissionCommandUseCase,
) : AnnouncementAssignmentCommandUseCase {
    override fun create(
        announcementAssignment: AnnouncementAssignment,
        assignment: Assignment,
    ): AnnouncementAssignment {
        val savedAnnouncementAssignment: AnnouncementAssignment =
            announcementAssignmentPersistencePort.save(
                announcementAssignment,
            )

        assignmentSubmissionCommandUseCase.initializeForMembers(assignment)
        return savedAnnouncementAssignment
    }
}
