package core.application.announcement.application.service

import core.application.announcement.application.exception.AssignmentNotFoundException
import core.application.member.application.service.MemberQueryService
import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.aggregate.AssignmentSubmission
import core.domain.announcement.port.inbound.AssignmentSubmissionCommandUseCase
import core.domain.announcement.port.outbound.AssignmentSubmissionPersistencePort
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class AssignmentSubmissionCommandService(
    val assignmentSubmissionPersistencePort: AssignmentSubmissionPersistencePort,
    val cohortQueryUseCase: CohortQueryUseCase,
    val memberQueryService: MemberQueryService,
) : AssignmentSubmissionCommandUseCase {
    override fun updateAssignmentSubmission(assignmentSubmission: AssignmentSubmission): AssignmentSubmission =
        assignmentSubmissionPersistencePort.save(assignmentSubmission)

    override fun ensureAssignmentSubmission(
        assignment: Assignment,
        memberId: MemberId,
    ): AssignmentSubmission {
        val assignmentId = assignment.id ?: throw AssignmentNotFoundException()
        return assignmentSubmissionPersistencePort.findByAssignmentIdAndMemberId(
            assignmentId = assignmentId,
            memberId = memberId,
        ) ?: assignmentSubmissionPersistencePort.save(
            AssignmentSubmission.create(
                assignmentId = assignmentId,
                memberId = memberId,
                teamId = memberQueryService.getMemberTeamId(memberId),
                submitType = assignment.submitType,
            ),
        )
    }

    override fun initializeForMembers(assignment: Assignment) {
        val latestCohortId: CohortId = cohortQueryUseCase.getLatestCohortId()
        memberQueryService
            .findAllMemberIdsByCohortIdAndAuthorityId(
                cohortId = latestCohortId,
                authorityId = 1,
            ).forEach { memberId ->
                ensureAssignmentSubmission(assignment, memberId)
            }
    }

    override fun initializeForNewCohortMember(
        memberId: MemberId,
        assignments: List<Assignment>,
    ) {
        assignments.forEach { assignment ->
            ensureAssignmentSubmission(assignment, memberId)
        }
    }
}
