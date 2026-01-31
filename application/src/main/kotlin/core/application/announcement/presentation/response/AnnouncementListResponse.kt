package core.application.announcement.presentation.response

import core.domain.announcement.port.outbound.query.AnnouncementListItemQueryModel

data class AnnouncementListResponse(
    val announcementCount: Int,
    val announcements: List<AnnouncementListItemResponse>,
) {
    companion object {
        fun from(queryModels: List<AnnouncementListItemQueryModel>): AnnouncementListResponse {
            return AnnouncementListResponse(
                announcementCount = queryModels.size,
                announcements =
                    queryModels.map { queryModel ->
                        AnnouncementListItemResponse.from(queryModel)
                    },
            )
        }
    }
}
