package core.application.announcement.application.service

import core.application.announcement.application.exception.AnnouncementReadNotFoundException
import core.domain.announcement.aggregate.AnnouncementRead
import core.domain.announcement.port.inbound.AnnouncementReadQueryUseCase
import core.domain.announcement.port.outbound.AnnouncementReadPersistencePort
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AnnouncementReadQueryService(
    val announcementReadPersistencePort: AnnouncementReadPersistencePort,
) : AnnouncementReadQueryUseCase {
    override fun getByAnnouncementIdAndMemberId(
        announcementId: AnnouncementId,
        memberId: MemberId,
    ): AnnouncementRead =
        announcementReadPersistencePort.findByAnnouncementIdAndMemberId(
            announcementId = announcementId,
            memberId = memberId,
        ) ?: throw AnnouncementReadNotFoundException()

    override fun existsByAnnouncementIdAndMemberId(
        announcementId: AnnouncementId,
        memberId: MemberId,
    ): Boolean =
        announcementReadPersistencePort.existsByAnnouncementIdAndMemberId(
            announcementId = announcementId,
            memberId = memberId,
        )

    override fun getByAnnouncementId(announcementId: AnnouncementId): List<AnnouncementRead> =
        announcementReadPersistencePort.findAllByAnnouncementId(announcementId)

    override fun countByAnnouncementId(announcementId: AnnouncementId): Int =
        announcementReadPersistencePort.countByAnnouncementId(announcementId)
}
