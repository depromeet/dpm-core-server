package core.persistence.announcement.repository

import core.domain.announcement.aggregate.AssignmentSubmission
import core.domain.announcement.port.outbound.AssignmentSubmissionPersistencePort
import core.domain.announcement.vo.AssignmentId
import core.domain.member.vo.MemberId
import core.entity.announcement.AssignmentSubmissionEntity
import org.springframework.stereotype.Repository

@Repository
class AssignmentSubmissionRepository(
    val assignmentSubmissionJpaRepository: AssignmentSubmissionJpaRepository,
) : AssignmentSubmissionPersistencePort {
    override fun save(assignmentSubmission: AssignmentSubmission): AssignmentSubmission =
        assignmentSubmissionJpaRepository.save(AssignmentSubmissionEntity.from(assignmentSubmission)).toDomain()

    override fun findByAssignmentIdAndMemberId(
        assignmentId: AssignmentId,
        memberId: MemberId,
    ): AssignmentSubmission? =
        assignmentSubmissionJpaRepository
            .findByAssignmentIdAndMemberId(assignmentId, memberId)
            ?.toDomain()

    override fun update(assignmentSubmission: AssignmentSubmission): AssignmentSubmission =
        assignmentSubmissionJpaRepository.save(AssignmentSubmissionEntity.from(assignmentSubmission)).toDomain()
}
