package core.entity.announcement

import core.domain.announcement.aggregate.AnnouncementRead
import core.domain.announcement.vo.AnnouncementId
import core.domain.announcement.vo.AnnouncementReadId
import core.domain.member.vo.MemberId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "announcement_reads",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_announcement_reads_announcement_member",
            columnNames = ["announcement_id", "member_id"],
        ),
    ],
)
class AnnouncementReadEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "announcement_read_id", nullable = false, updatable = false)
    val id: Long,
    @Column(name = "announcement_id", nullable = false)
    val announcementId: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "read_at")
    val readAt: Instant?,
) {
    companion object {
        fun from(announcementRead: AnnouncementRead): AnnouncementReadEntity =
            AnnouncementReadEntity(
                id = announcementRead.id?.value ?: 0L,
                announcementId = announcementRead.announcementId.value,
                memberId = announcementRead.memberId.value,
                readAt = announcementRead.readAt,
            )
    }

    fun toDomain(): AnnouncementRead =
        AnnouncementRead(
            id = AnnouncementReadId(id),
            announcementId = AnnouncementId(announcementId),
            memberId = MemberId(memberId),
            readAt = readAt,
        )
}
