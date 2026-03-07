package core.domain.announcement.port.inbound

import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.aggregate.AssignmentSubmission
import core.domain.member.vo.MemberId

interface AssignmentSubmissionCommandUseCase {
    fun updateAssignmentSubmission(assignmentSubmission: AssignmentSubmission): AssignmentSubmission

    fun initializeForMembers(assignment: Assignment)

    fun initializeForNewCohortMember(
        memberId: MemberId,
        assignments: List<Assignment>,
    )
}
