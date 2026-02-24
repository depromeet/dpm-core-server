package core.application.gathering.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.gathering.presentation.request.CreateGatheringV2ByInviteTagNamesRequest
import core.application.gathering.presentation.request.CreateGatheringV2Request
import core.application.gathering.presentation.request.UpdateGatheringV2Request
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "GatheringV2", description = "회식 참여 조사 API")
interface GatheringV2CommandApi {
    @ApiResponse(
        responseCode = "200",
        description = "회식 참여 조사 추가 성공",
    )
    @Operation(
        summary = "회식 참여 조사 생성 API",
        description = "회식 참여 조사를 생성하는 API입니다",
    )
    fun createGatheringV2(
        createGatheringV2Request: CreateGatheringV2Request,
        memberId: MemberId,
    ): CustomResponse<Void>

    @ApiResponse(
        responseCode = "200",
        description = "회식 초대 추가 성공 (inviteTagName 기반)",
    )
    @Operation(
        summary = "회식 초대 추가 API (inviteTagName 기반)",
        description = "inviteTagName 목록을 이용해 회식 초대를 추가하는 API입니다",
    )
    fun createGatheringV2ByInviteTagNames(
        createGatheringV2ByInviteTagNamesRequest: CreateGatheringV2ByInviteTagNamesRequest,
        memberId: MemberId,
    ): CustomResponse<Void>

    @ApiResponse(
        responseCode = "200",
        description = "회식 참여 조사 변경 성공",
    )
    @Operation(
        summary = "회식 참여 조사 업데이트 API",
        description = "회식 참여 조사를 업데이트하는 API입니다",
    )
    fun updateGatheringV2(
        updateGatheringV2Request: UpdateGatheringV2Request,
        memberId: MemberId,
        gatheringId: GatheringV2Id,
    ): CustomResponse<Void>
}
