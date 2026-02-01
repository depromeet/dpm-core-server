package core.persistence.announcement.repository

import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId
import core.entity.announcement.AnnouncementReadEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AnnouncementReadJpaRepository : JpaRepository<AnnouncementReadEntity, Long> {
    fun findByAnnouncementIdAndMemberId(
        announcementId: AnnouncementId,
        memberId: MemberId,
    ): AnnouncementReadEntity?

    fun existsByMemberIdAndAnnouncementId(
        memberId: MemberId,
        announcementId: AnnouncementId,
    ): Boolean

    fun findAllByAnnouncementId(announcementId: AnnouncementId): List<AnnouncementReadEntity>

    fun countByAnnouncementId(announcementId: AnnouncementId): Int
}
