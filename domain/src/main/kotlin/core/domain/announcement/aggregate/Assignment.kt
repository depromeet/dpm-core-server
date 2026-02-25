package core.domain.announcement.aggregate

import core.domain.announcement.enums.SubmitType
import core.domain.announcement.vo.AssignmentId
import java.time.Instant

class Assignment(
    val id: AssignmentId? = null,
    val submitType: SubmitType?,
    val startAt: Instant?,
    val dueAt: Instant?,
    val submitLink: String?,
    val createdAt: Instant? = null,
    updatedAt: Instant? = null,
    deletedAt: Instant? = null,
) {
    var updatedAt: Instant? = updatedAt
        private set

    var deletedAt: Instant? = deletedAt
        private set

    fun isDeleted(): Boolean = deletedAt != null

    companion object {
        fun create(
            submitType: SubmitType?,
            startAt: Instant? = null,
            dueAt: Instant? = null,
            submitLink: String?,
        ): Assignment {
            val now = Instant.now()

            return Assignment(
                submitType = submitType,
                startAt = startAt,
                dueAt = dueAt,
                submitLink = submitLink,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
