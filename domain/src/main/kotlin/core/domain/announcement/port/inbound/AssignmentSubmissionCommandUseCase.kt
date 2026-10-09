package core.domain.announcement.port.inbound

import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.aggregate.AssignmentSubmission
import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId

interface AssignmentSubmissionCommandUseCase {
    fun updateAssignmentSubmission(assignmentSubmission: AssignmentSubmission): AssignmentSubmission

    fun ensureAssignmentSubmission(
        assignment: Assignment,
        memberId: MemberId,
    ): AssignmentSubmission

    fun initializeForMembers(assignment: Assignment)

    @Deprecated("과제는 현재 MVP에서 사용하지 않습니다. 2차 MVP 검토 전까지 기존 승인 초기화 동작을 유지합니다.")
    fun initializeForNewCohortMember(
        memberId: MemberId,
        assignments: List<Assignment>,
        cohortId: CohortId,
    )
}
