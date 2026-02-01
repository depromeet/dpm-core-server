package core.domain.announcement.aggregate

import core.domain.announcement.enums.SubmitStatus
import core.domain.announcement.enums.SubmitType
import core.domain.announcement.vo.AssignmentId
import core.domain.announcement.vo.AssignmentSubmissionId
import core.domain.member.vo.MemberId
import core.domain.team.vo.TeamId
import java.time.Instant

class AssignmentSubmission(
    val id: AssignmentSubmissionId? = null,
    val assignmentId: AssignmentId,
    val memberId: MemberId,
    val teamId: TeamId,
    submitType: SubmitType,
    submitStatus: SubmitStatus,
    val score: Int,
    val dueAt: Instant,
    val createdAt: Instant? = null,
    updatedAt: Instant? = null,
    deletedAt: Instant? = null,
) {
    var submitType: SubmitType = submitType
        private set

    var submitStatus: SubmitStatus = submitStatus
        private set

    var updatedAt: Instant? = updatedAt
        private set

    var deletedAt: Instant? = deletedAt
        private set

    fun isDeleted(): Boolean = deletedAt != null

    fun updateSubmitStatus(newSubmitStatus: SubmitStatus): AssignmentSubmission =
        AssignmentSubmission(
            id = id,
            assignmentId = assignmentId,
            memberId = memberId,
            teamId = teamId,
            submitType = submitType,
            submitStatus = newSubmitStatus,
            score = score,
            dueAt = dueAt,
            createdAt = createdAt,
            updatedAt = Instant.now(),
            deletedAt = deletedAt,
        )
}
