package core.application.announcement.presentation.controller

import core.application.announcement.application.service.AnnouncementQueryService
import core.application.announcement.presentation.response.AnnouncementDetailResponse
import core.application.announcement.presentation.response.AnnouncementListResponse
import core.application.common.exception.CustomResponse
import core.domain.announcement.vo.AnnouncementId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/announcements")
class AnnouncementQueryController(
    val announcementQueryService: AnnouncementQueryService,
) : AnnouncementQueryApi {
    @GetMapping
    override fun getAnnouncementList(): CustomResponse<AnnouncementListResponse> =
        CustomResponse.ok(announcementQueryService.getAllAnnouncements())

    @GetMapping("/{announcementId}")
    override fun getAnnouncementDetail(
        @RequestParam announcementId: AnnouncementId,
    ): CustomResponse<AnnouncementDetailResponse> =
        CustomResponse.ok(announcementQueryService.getAnnouncementDetail(announcementId))

    @GetMapping("/{announcementId}/mark-as-read/members")
    override fun getAnnouncementReadMemberList(
        announcementId: AnnouncementId,
    ): CustomResponse<AnnouncementDetailResponse> {
        TODO("Not yet implemented")
    }
}
