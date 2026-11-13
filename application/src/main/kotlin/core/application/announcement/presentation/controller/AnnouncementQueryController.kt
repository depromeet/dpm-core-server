package core.application.announcement.presentation.controller

import core.application.announcement.application.service.AnnouncementQueryService
import core.application.announcement.presentation.response.AnnouncementDetailResponse
import core.application.announcement.presentation.response.AnnouncementListResponse
import core.application.announcement.presentation.response.AnnouncementViewMemberListResponse
import core.application.announcement.presentation.response.AssignmentStatusMemberListResponse
import core.application.common.exception.CustomResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.announcement.vo.AnnouncementId
import core.domain.member.vo.MemberId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/announcements")
class AnnouncementQueryController(
    val announcementQueryService: AnnouncementQueryService,
) : AnnouncementQueryApi {
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    override fun getAnnouncementList(): CustomResponse<AnnouncementListResponse> =
        CustomResponse.ok(announcementQueryService.getAllAnnouncements())

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{announcementId}")
    override fun getAnnouncementDetail(
        @PathVariable announcementId: AnnouncementId,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<AnnouncementDetailResponse> =
        CustomResponse.ok(announcementQueryService.getAnnouncementDetail(announcementId, memberId))

    @PreAuthorize("hasAuthority('update:member')")
    @GetMapping("/{announcementId}/mark-as-read/members")
    override fun getAnnouncementReadMemberList(
        @PathVariable announcementId: AnnouncementId,
    ): CustomResponse<AnnouncementViewMemberListResponse> =
        CustomResponse.ok(announcementQueryService.getAnnouncementReadMemberList(announcementId))

    @PreAuthorize("hasAuthority('update:member')")
    @GetMapping("/{announcementId}/assignment-status")
    override fun getAssignmentStatusMemberList(
        @PathVariable announcementId: AnnouncementId,
    ): CustomResponse<AssignmentStatusMemberListResponse> =
        CustomResponse.ok(announcementQueryService.getAssignmentStatusMemberList(announcementId))
}
