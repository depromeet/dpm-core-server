package core.domain.announcement.aggregate

import core.domain.announcement.enums.SubmitType
import core.domain.announcement.vo.AssignmentId
import java.time.Instant

class Assignment(
    val id: AssignmentId? = null,
    val submitType: SubmitType,
    val startAt: Instant,
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
