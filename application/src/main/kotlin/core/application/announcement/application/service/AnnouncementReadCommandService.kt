package core.application.announcement.application.service

import core.domain.announcement.aggregate.AnnouncementRead
import core.domain.announcement.port.inbound.AnnouncementReadCommandUseCase
import core.domain.announcement.port.inbound.AnnouncementReadQueryUseCase
import core.domain.announcement.port.outbound.AnnouncementReadPersistencePort
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId
import org.springframework.stereotype.Service

@Service
class AnnouncementReadCommandService(
    val announcementReadPersistencePort: AnnouncementReadPersistencePort,
    val announcementReadQueryUseCase: AnnouncementReadQueryUseCase,
) : AnnouncementReadCommandUseCase {
    override fun markAsRead(announcementRead: AnnouncementRead) =
        announcementReadPersistencePort.save(announcementRead.markAsRead())

    override fun initializeForMembers(
        announcementId: AnnouncementId,
        memberIds: List<MemberId>,
    ) {
        val initializeAnnouncementReads =
            memberIds
                .distinct()
                .map { memberId ->
                    AnnouncementRead.createUnread(
                        announcementId = announcementId,
                        memberId = memberId,
                    )
                }.filter { announcementRead ->
                    announcementReadPersistencePort.findByAnnouncementIdAndMemberId(
                        announcementId = announcementId,
                        memberId = announcementRead.memberId,
                    ) == null
                }
        announcementReadPersistencePort.saveAll(initializeAnnouncementReads)
    }

    override fun create(
        announcementId: AnnouncementId,
        memberId: MemberId,
    ): AnnouncementRead = announcementReadPersistencePort.save(AnnouncementRead.create(announcementId, memberId))

    override fun initializeForNewCohortMember(
        memberId: MemberId,
        announcementIds: List<AnnouncementId>,
    ) {
        val initializeAnnouncementReads =
            announcementIds.map { announcementId ->
                AnnouncementRead.createUnread(
                    announcementId = announcementId,
                    memberId = memberId,
                )
            }.filter { announcementRead ->
                announcementReadPersistencePort.findByAnnouncementIdAndMemberId(
                    announcementId = announcementRead.announcementId,
                    memberId = announcementRead.memberId,
                ) == null
            }
        announcementReadPersistencePort.saveAll(initializeAnnouncementReads)
    }
}
