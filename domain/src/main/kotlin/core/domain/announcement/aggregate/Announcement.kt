package core.domain.announcement.aggregate

import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId
import java.time.Instant

class Announcement(
    val id: AnnouncementId? = null,
    val announcementType: AnnouncementType,
    val title: String,
    val content: String?,
    val authorId: MemberId,
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
