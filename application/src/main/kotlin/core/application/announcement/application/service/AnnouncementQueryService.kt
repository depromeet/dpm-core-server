package core.application.announcement.application.service

import core.application.announcement.application.exception.AnnouncementNotFoundException
import core.application.announcement.presentation.response.AnnouncementListResponse
import core.domain.announcement.aggregate.Announcement
import core.domain.announcement.port.inbound.AnnouncementQueryUseCase
import core.domain.announcement.port.outbound.AnnouncementPersistencePort
import core.domain.announcement.port.outbound.query.AnnouncementListItemQueryModel
import core.domain.announcement.vo.AnnouncementId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AnnouncementQueryService(
    val announcementPersistencePort: AnnouncementPersistencePort,
) : AnnouncementQueryUseCase {
    fun getAllAnnouncements(): AnnouncementListResponse {
        val announcementListItemQueryModels: List<AnnouncementListItemQueryModel> =
            announcementPersistencePort.findAnnouncementListItems()

        return AnnouncementListResponse.from(announcementListItemQueryModels)
    }

    override fun getAnnouncementById(announcementId: AnnouncementId): Announcement =
        announcementPersistencePort.findAnnouncementById(announcementId) ?: throw AnnouncementNotFoundException()
}
