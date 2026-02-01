package core.entity.announcement

import core.domain.announcement.aggregate.AnnouncementAssignment
import core.domain.announcement.vo.AnnouncementAssignmentId
import core.domain.announcement.vo.AnnouncementId
import core.domain.announcement.vo.AssignmentId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "announcement_assignments",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_announcement_assignments_assignment_announcement",
            columnNames = ["assignment_id", "announcement_id"],
        ),
    ],
)
class AnnouncementAssignmentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "announcement_assignment_id", nullable = false, updatable = false)
    val id: Long,
    @Column(name = "assignment_id", nullable = false)
    val assignmentId: Long,
    @Column(name = "announcement_id", nullable = false)
    val announcementId: Long,
) {
    companion object {
        fun from(announcementAssignment: AnnouncementAssignment): AnnouncementAssignmentEntity =
            AnnouncementAssignmentEntity(
                id = announcementAssignment.id?.value ?: 0L,
                assignmentId = announcementAssignment.assignmentId.value,
                announcementId = announcementAssignment.announcementId.value,
            )
    }

    fun toDomain(): AnnouncementAssignment =
        AnnouncementAssignment(
            id = AnnouncementAssignmentId(id),
            assignmentId = AssignmentId(assignmentId),
            announcementId = AnnouncementId(announcementId),
        )
}
