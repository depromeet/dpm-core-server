package core.domain.announcement.aggregate

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
    val submitType: SubmitType,
    val dueAt: Instant,
    val createdAt: Instant? = null,
    updatedAt: Instant? = null,
    deletedAt: Instant? = null,
) {
    var updatedAt: Instant? = updatedAt
        private set

    var deletedAt: Instant? = deletedAt
        private set

    fun isDeleted(): Boolean = deletedAt != null
}
