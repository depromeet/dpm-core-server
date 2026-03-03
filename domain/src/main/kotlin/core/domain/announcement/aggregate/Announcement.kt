package core.domain.announcement.aggregate

import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId
import java.time.Instant

class Announcement(
    val id: AnnouncementId? = null,
    val announcementType: AnnouncementType,
    title: String,
    content: String?,
    val authorId: MemberId,
    val createdAt: Instant? = null,
    updatedAt: Instant? = null,
    deletedAt: Instant? = null,
) {
    var title: String = title
        private set

    var content: String? = content
        private set

    var updatedAt: Instant? = updatedAt
        private set

    var deletedAt: Instant? = deletedAt
        private set

    fun isDeleted(): Boolean = deletedAt != null

    fun markAsDeleted() {
        deletedAt = Instant.now()
    }

    fun update(
        title: String,
        content: String?,
    ) {
        this.title = title
        this.content = content
        this.updatedAt = Instant.now()
    }

    companion object {
        fun create(
            announcementType: AnnouncementType,
            title: String,
            content: String?,
            authorId: MemberId,
        ): Announcement {
            val now = Instant.now()

            return Announcement(
                announcementType = announcementType,
                title = title,
                content = content,
                authorId = authorId,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
