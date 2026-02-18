package core.application.announcement.presentation.response

import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.domain.announcement.enums.AnnouncementType
import core.domain.announcement.enums.SubmitType
import core.domain.announcement.port.outbound.query.AnnouncementListItemQueryModel
import core.domain.announcement.vo.AnnouncementId
import java.time.LocalDateTime

data class AnnouncementListItemResponse(
    val announcementId: AnnouncementId,
    val title: String,
    val announcementType: AnnouncementType,
    val assignmentType: SubmitType?,
    val createdAt: LocalDateTime,
    val readMemberCount: Int,
) {
    companion object {
        fun from(queryModel: AnnouncementListItemQueryModel): AnnouncementListItemResponse =
            AnnouncementListItemResponse(
                announcementId = queryModel.announcementId,
                title = queryModel.title,
                announcementType = queryModel.announcementType,
                assignmentType = queryModel.submitType,
                createdAt = instantToLocalDateTime(queryModel.createdAt),
                readMemberCount = queryModel.readMemberCount ?: 0,
            )
    }
}
