package core.application.announcement.presentation.controller

import core.application.announcement.presentation.request.CreateAnnouncementRequest
import core.application.common.exception.CustomResponse
import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Announcement", description = "공지/과제 API")
interface AnnouncementCommandApi {
    @ApiResponse(
        responseCode = "200",
        description = "공지/과제 추가 성공",
    )
    @Operation(
        summary = "공지/과제 추가 API",
        description = "공지나 과제를 작성하는 API입니다",
    )
    fun createAnnouncement(
        createAnnouncementRequest: CreateAnnouncementRequest,
        memberId: MemberId,
    ): CustomResponse<Void>
}
