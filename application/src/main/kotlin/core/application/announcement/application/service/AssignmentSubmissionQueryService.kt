package core.application.announcement.application.service

import core.application.announcement.application.exception.AssignmentSubmissionNotFoundException
import core.domain.announcement.aggregate.AssignmentSubmission
import core.domain.announcement.port.inbound.AssignmentSubmissionQueryUseCase
import core.domain.announcement.port.outbound.AssignmentSubmissionPersistencePort
import core.domain.announcement.vo.AssignmentId
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class AssignmentSubmissionQueryService(
    val assignmentSubmissionPersistencePort: AssignmentSubmissionPersistencePort,
) : AssignmentSubmissionQueryUseCase {
    override fun getByAssignmentIdAndMemberId(
        assignmentId: AssignmentId,
        memberId: MemberId,
    ): AssignmentSubmission =
        assignmentSubmissionPersistencePort.findByAssignmentIdAndMemberId(
            assignmentId = assignmentId,
            memberId = memberId,
        ) ?: throw AssignmentSubmissionNotFoundException()
}
