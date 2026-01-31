package core.application.announcement.presentation.controller

import core.application.announcement.application.service.AnnouncementQueryService
import core.application.announcement.presentation.response.AnnouncementListResponse
import core.application.common.exception.CustomResponse
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/announcements")
class AnnouncementQueryController(
    val announcementQueryService: AnnouncementQueryService,
) : AnnouncementQueryApi {
    override fun getAnnouncementList(): CustomResponse<AnnouncementListResponse> =
        CustomResponse.ok(announcementQueryService.getAllAnnouncements())
}
