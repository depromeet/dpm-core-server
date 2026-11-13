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
    // 디프만 앱 홈이 서버 렌더링에서 토큰 없이 미리 불러온다. 활성 기수 공지 목록만 주고 회원 정보는 없다.
    @PreAuthorize("permitAll()")
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
