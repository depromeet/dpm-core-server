package core.application.announcement.application.service

import core.domain.announcement.aggregate.AnnouncementRead
import core.domain.announcement.port.inbound.AnnouncementReadCommandUseCase
import core.domain.announcement.port.outbound.AnnouncementReadPersistencePort
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class AnnouncementReadCommandService(
    val announcementReadPersistencePort: AnnouncementReadPersistencePort,
) : AnnouncementReadCommandUseCase {
    override fun markAsRead(announcementRead: AnnouncementRead) =
        announcementReadPersistencePort.save(announcementRead.markAsRead())

    override fun initializeForMembers(
        announcementId: AnnouncementId,
        memberIds: List<MemberId>,
    ) {
        val initializeAnnouncementReads =
            memberIds.map { memberId ->
                AnnouncementRead.createUnread(
                    announcementId = announcementId,
                    memberId = memberId,
                )
            }
        announcementReadPersistencePort.saveAll(initializeAnnouncementReads)
    }
}
