package core.domain.announcement.event

import core.domain.announcement.aggregate.Announcement
import core.domain.cohort.vo.CohortId

data class AnnouncementRemindEvent(
    val announcement: Announcement,
    val cohortId: CohortId,
) {
    companion object {
        fun of(
            announcement: Announcement,
            cohortId: CohortId,
        ): AnnouncementRemindEvent =
            AnnouncementRemindEvent(
                announcement = announcement,
                cohortId = cohortId,
            )
    }
}
