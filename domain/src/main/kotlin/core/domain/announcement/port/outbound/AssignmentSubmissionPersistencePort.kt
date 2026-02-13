package core.domain.announcement.port.outbound

import core.domain.announcement.aggregate.AssignmentSubmission
import core.domain.announcement.vo.AssignmentId
import core.domain.member.vo.MemberId

interface AssignmentSubmissionPersistencePort {
    fun save(assignmentSubmission: AssignmentSubmission): AssignmentSubmission

    fun findByAssignmentIdAndMemberId(
        assignmentId: AssignmentId,
        memberId: MemberId,
    ): AssignmentSubmission?

    fun update(assignmentSubmission: AssignmentSubmission): AssignmentSubmission
}
