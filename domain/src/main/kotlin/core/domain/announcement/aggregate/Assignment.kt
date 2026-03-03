package core.domain.announcement.aggregate

import core.domain.announcement.enums.SubmitType
import core.domain.announcement.vo.AssignmentId
import java.time.Instant

class Assignment(
    val id: AssignmentId? = null,
    submitType: SubmitType,
    startAt: Instant?,
    dueAt: Instant?,
    submitLink: String?,
    val createdAt: Instant? = null,
    updatedAt: Instant? = null,
    deletedAt: Instant? = null,
) {
    var submitType: SubmitType = submitType
        private set

    var startAt: Instant? = startAt
        private set

    var dueAt: Instant? = dueAt
        private set

    var submitLink: String? = submitLink
        private set

    var updatedAt: Instant? = updatedAt
        private set

    var deletedAt: Instant? = deletedAt
        private set

    fun isDeleted(): Boolean = deletedAt != null

    fun update(
        submitType: SubmitType?,
        startAt: Instant?,
        dueAt: Instant?,
        submitLink: String?,
    ) {
        this.submitType = submitType ?: this.submitType
        this.startAt = startAt
        this.dueAt = dueAt
        this.submitLink = submitLink
        this.updatedAt = Instant.now()
    }

    companion object {
        fun create(
            submitType: SubmitType,
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
