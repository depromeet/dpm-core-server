package core.domain.announcement.port.inbound

import core.domain.announcement.aggregate.AssignmentSubmission
import core.domain.announcement.vo.AssignmentId
import core.domain.member.vo.MemberId

interface AssignmentSubmissionQueryUseCase {
    fun getByAssignmentIdAndMemberId(
        assignmentId: AssignmentId,
        memberId: MemberId,
    ): AssignmentSubmission
}
