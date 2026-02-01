package core.application.announcement.application.service

import core.application.announcement.application.exception.AnnouncementNotFoundException
import core.application.announcement.presentation.response.AnnouncementDetailResponse
import core.application.announcement.presentation.response.AnnouncementListResponse
import core.application.session.presentation.mapper.TimeMapper.instantToLocalDateTime
import core.domain.announcement.aggregate.Announcement
import core.domain.announcement.port.inbound.AnnouncementQueryUseCase
import core.domain.announcement.port.inbound.AnnouncementReadQueryUseCase
import core.domain.announcement.port.outbound.AnnouncementPersistencePort
import core.domain.announcement.port.outbound.query.AnnouncementListItemQueryModel
import core.domain.announcement.vo.AnnouncementId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AnnouncementQueryService(
    val announcementPersistencePort: AnnouncementPersistencePort,
    val announcementReadQueryUseCase: AnnouncementReadQueryUseCase,
) : AnnouncementQueryUseCase {
    fun getAllAnnouncements(): AnnouncementListResponse {
        val announcementListItemQueryModels: List<AnnouncementListItemQueryModel> =
            announcementPersistencePort.findAnnouncementListItems()

        return AnnouncementListResponse.from(announcementListItemQueryModels)
    }

    override fun getAnnouncementById(announcementId: AnnouncementId): Announcement =
        announcementPersistencePort.findAnnouncementById(announcementId) ?: throw AnnouncementNotFoundException()

    fun getAnnouncementDetail(announcementId: AnnouncementId): AnnouncementDetailResponse {
        val announcement: Announcement = getAnnouncementById(announcementId)
        val announcementReadCount: Int =
            announcementReadQueryUseCase.countByAnnouncementId(announcementId)

        return AnnouncementDetailResponse.of(
            title = announcement.title,
            content = announcement.content,
            createdAt = instantToLocalDateTime(announcement.createdAt!!),
            markAsReadCount = announcementReadCount,
        )
    }
}
