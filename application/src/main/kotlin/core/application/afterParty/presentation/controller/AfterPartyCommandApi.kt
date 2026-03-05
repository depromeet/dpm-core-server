package core.application.afterParty.presentation.controller

import core.application.afterParty.presentation.request.CreateAfterPartyByInviteTagNamesRequest
import core.application.afterParty.presentation.request.CreateAfterPartyRequest
import core.application.afterParty.presentation.request.UpdateAfterPartyRequest
import core.application.common.exception.CustomResponse
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "AfterParty", description = "회식 참여 조사 API")
interface AfterPartyCommandApi {
    @ApiResponse(
        responseCode = "200",
        description = "회식 참여 조사 추가 성공",
    )
    @Operation(
        summary = "회식 참여 조사 생성 API",
        description = "회식 참여 조사를 생성하는 API입니다",
    )
    fun createAfterParty(
        createAfterPartyRequest: CreateAfterPartyRequest,
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
    fun createAfterPartyByInviteTagNames(
        createAfterPartyByInviteTagNamesRequest: CreateAfterPartyByInviteTagNamesRequest,
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
    fun updateAfterParty(
        updateAfterPartyRequest: UpdateAfterPartyRequest,
        memberId: MemberId,
        afterPartyId: AfterPartyId,
    ): CustomResponse<Void>
}
