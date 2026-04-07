package core.domain.announcement.event

import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.vo.AnnouncementId
import core.domain.cohort.vo.CohortId

data class AnnouncementCreatedEvent(
    val announcementId: AnnouncementId,
    val announcementType: AnnouncementType,
    val title: String,
    val cohortId: CohortId,
) {
    companion object {
        fun of(
            announcementId: AnnouncementId,
            announcementType: AnnouncementType,
            title: String,
            cohortId: CohortId,
        ): AnnouncementCreatedEvent =
            AnnouncementCreatedEvent(
                announcementId = announcementId,
                announcementType = announcementType,
                title = title,
                cohortId = cohortId,
            )
    }
}
