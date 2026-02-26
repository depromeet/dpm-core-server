package core.entity.announcement

import core.domain.announcement.aggregate.AssignmentSubmission
import core.domain.announcement.enums.SubmitStatus
import core.domain.announcement.enums.SubmitType
import core.domain.announcement.vo.AssignmentId
import core.domain.announcement.vo.AssignmentSubmissionId
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "assignment_submissions")
class AssignmentSubmissionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id", nullable = false, updatable = false)
    val id: Long,
    @Column(name = "assignment_id", nullable = false)
    val assignmentId: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "team_id", nullable = false)
    val teamId: Long,
    @Column(name = "submit_type", nullable = false)
    val submitType: Int,
    @Column(name = "submit_status", nullable = false)
    val submitStatus: Int,
    @Column(name = "score")
    val score: Int?,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
) {
    companion object {
        fun from(submission: AssignmentSubmission): AssignmentSubmissionEntity =
            AssignmentSubmissionEntity(
                id = submission.id?.value ?: 0L,
                assignmentId = submission.assignmentId.value,
                memberId = submission.memberId.value,
                teamId = submission.teamId.value,
                submitType = submission.submitType.value,
                submitStatus = submission.submitStatus.value,
                score = submission.score,
                createdAt = submission.createdAt ?: Instant.now(),
                updatedAt = submission.updatedAt ?: Instant.now(),
                deletedAt = submission.deletedAt,
            )
    }

    fun toDomain(): AssignmentSubmission =
        AssignmentSubmission(
            id = AssignmentSubmissionId(id),
            assignmentId = AssignmentId(assignmentId),
            memberId = MemberId(memberId),
            teamId = TeamId(teamId),
            submitType = SubmitType.fromValue(submitType),
            submitStatus = SubmitStatus.fromValue(submitStatus),
            score = score,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )
}
