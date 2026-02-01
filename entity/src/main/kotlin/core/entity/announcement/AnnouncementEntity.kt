package core.entity.announcement

import core.domain.announcement.aggregate.Announcement
import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "announcements")
class AnnouncementEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "announcement_id", nullable = false, updatable = false)
    val id: Long,
    @Column(name = "announcement_type", nullable = false)
    val announcementType: Int,
    @Column(name = "title", nullable = false)
    val title: String,
    @Column(name = "content", columnDefinition = "longtext")
    val content: String?,
    @Column(name = "author_id", nullable = false)
    val authorId: Long,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant,
    @Column(name = "deleted_at")
    val deletedAt: Instant? = null,
) {
    companion object {
        fun from(announcement: Announcement): AnnouncementEntity =
            AnnouncementEntity(
                id = announcement.id?.value ?: 0L,
                announcementType = announcement.announcementType.value,
                title = announcement.title,
                content = announcement.content,
                authorId = announcement.authorId.value,
                createdAt = announcement.createdAt ?: Instant.now(),
                updatedAt = announcement.updatedAt ?: Instant.now(),
                deletedAt = announcement.deletedAt,
            )
    }

    fun toDomain(): Announcement =
        Announcement(
            id = AnnouncementId(id),
            announcementType = AnnouncementType.fromValue(announcementType),
            title = title,
            content = content,
            authorId = MemberId(authorId),
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
        )
}
