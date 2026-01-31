package core.persistence.announcement.repository

import core.domain.announcement.aggregate.AnnouncementRead
import core.domain.announcement.port.outbound.AnnouncementReadPersistencePort
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId
import core.entity.announcement.AnnouncementReadEntity
import org.springframework.stereotype.Repository

@Repository
class AnnouncementReadRepository(
    val announcementReadJpaRepository: AnnouncementReadJpaRepository,
) : AnnouncementReadPersistencePort {
    override fun save(announcementRead: AnnouncementRead): AnnouncementRead =
        announcementReadJpaRepository.save(AnnouncementReadEntity.from(announcementRead)).toDomain()

    override fun findByAnnouncementIdAndMemberId(
        announcementId: AnnouncementId,
        memberId: MemberId,
    ): AnnouncementRead? =
        announcementReadJpaRepository.findByAnnouncementIdAndMemberId(
            announcementId = announcementId,
            memberId = memberId,
        )?.toDomain()

    override fun existsByAnnouncementIdAndMemberId(
        announcementId: AnnouncementId,
        memberId: MemberId,
    ): Boolean = announcementReadJpaRepository.existsByMemberIdAndAnnouncementId(memberId, announcementId)
}
