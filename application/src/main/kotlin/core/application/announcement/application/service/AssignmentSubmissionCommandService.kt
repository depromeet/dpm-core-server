package core.application.announcement.application.service

import core.domain.announcement.aggregate.AssignmentSubmission
import core.domain.announcement.port.inbound.AssignmentSubmissionCommandUseCase
import core.domain.announcement.port.outbound.AssignmentSubmissionPersistencePort
import org.springframework.stereotype.Service

@Service
class AssignmentSubmissionCommandService(
    val assignmentSubmissionPersistencePort: AssignmentSubmissionPersistencePort,
) : AssignmentSubmissionCommandUseCase {
    override fun updateAssignmentSubmission(assignmentSubmission: AssignmentSubmission): AssignmentSubmission =
        assignmentSubmissionPersistencePort.save(assignmentSubmission)
}
