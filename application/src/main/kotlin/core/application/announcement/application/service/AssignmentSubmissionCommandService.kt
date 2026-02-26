package core.application.announcement.application.service

import core.application.member.application.service.MemberQueryService
import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.aggregate.AssignmentSubmission
import core.domain.announcement.port.inbound.AssignmentQueryUseCase
import core.domain.announcement.port.inbound.AssignmentSubmissionCommandUseCase
import core.domain.announcement.port.outbound.AssignmentSubmissionPersistencePort
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.team.vo.TeamId
import org.springframework.stereotype.Service

@Service
class AssignmentSubmissionCommandService(
    val assignmentSubmissionPersistencePort: AssignmentSubmissionPersistencePort,
    val assignmentQueryUseCase: AssignmentQueryUseCase,
    val cohortQueryUseCase: CohortQueryUseCase,
    val memberQueryService: MemberQueryService,
) : AssignmentSubmissionCommandUseCase {
    override fun updateAssignmentSubmission(assignmentSubmission: AssignmentSubmission): AssignmentSubmission =
        assignmentSubmissionPersistencePort.save(assignmentSubmission)

    override fun addDeeperInvitationsToSubmission(assignment: Assignment) {
        val latestCohortId: CohortId = cohortQueryUseCase.getLatestCohortId()
        memberQueryService
            .findAllMemberIdsByCohortIdAndAuthorityId(
                cohortId = latestCohortId,
                authorityId = 1,
            ).forEach { memberId ->
                val teamId: TeamId = memberQueryService.getMemberTeamId(memberId)
                assignmentSubmissionPersistencePort.findByAssignmentIdAndMemberId(
                    assignmentId = assignment.id!!,
                    memberId = memberId,
                ) ?: assignmentSubmissionPersistencePort.save(
                    AssignmentSubmission.create(
                        assignmentId = assignment.id!!,
                        memberId = memberId,
                        teamId = teamId,
                        submitType = assignment.submitType,
                    ),
                )
            }
    }
}
