package core.entity.announcement

import core.domain.announcement.aggregate.Assignment
import core.domain.announcement.enums.SubmitType
import core.domain.announcement.vo.AssignmentId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "assignments")
class AssignmentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id", nullable = false, updatable = false)
    val id: Long,
    @Column(name = "submit_type", nullable = false)
    val submitType: Int,
    @Column(name = "start_at")
    val startAt: Instant?,
    @Column(name = "due_at")
    val dueAt: Instant?,
    @Column(name = "submitLink")
    val submitLink: String?,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
) {
    companion object {
        fun from(assignment: Assignment): AssignmentEntity =
            AssignmentEntity(
                id = assignment.id?.value ?: 0L,
                submitType = assignment.submitType.value,
                startAt = assignment.startAt,
                dueAt = assignment.dueAt,
                submitLink = assignment.submitLink,
                createdAt = assignment.createdAt ?: Instant.now(),
                updatedAt = assignment.updatedAt ?: Instant.now(),
                deletedAt = assignment.deletedAt,
            )
    }

    fun toDomain(): Assignment =
        Assignment(
            id = AssignmentId(id),
            submitType = SubmitType.fromValue(submitType),
            startAt = startAt,
            dueAt = dueAt,
            submitLink = submitLink,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )
}
