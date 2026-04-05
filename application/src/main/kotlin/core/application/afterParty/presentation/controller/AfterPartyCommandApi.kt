package core.application.afterParty.presentation.controller

import core.application.afterParty.presentation.request.CreateAfterPartyByInviteTagNamesRequest
import core.application.afterParty.presentation.request.CreateAfterPartyRequest
import core.application.afterParty.presentation.request.SendNotificationUnMarkedRsvpRequest
import core.application.afterParty.presentation.request.UpdateAfterPartyRequest
import core.application.afterParty.presentation.response.AfterPartyInviteeCompensationResponse
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

    @ApiResponse(
        responseCode = "200",
        description = "열려 있는 회식 초대 보상 처리 성공",
    )
    @Operation(
        summary = "열려 있는 회식 초대 보상 API (dev)",
        description = "누락된 after_party_invitees를 보상 생성합니다. closedAt이 지나지 않은 회식만 대상입니다.",
    )
    fun compensateMissingInviteesForOpenAfterParties(): CustomResponse<AfterPartyInviteeCompensationResponse>

    @ApiResponse(
        responseCode = "200",
        description = "회식 참여 여부 미제출자 알림 발송 성공",
    )
    @Operation(
        summary = "회식 참여 여부 미제출자 알림 발송 API",
        description = "회식 참여 여부 미제출자에 대해서 알림 발송을 발송합니다.",
    )
    fun sendNotificationUnMarkedRsvp(
        afterPartyId: AfterPartyId,
        sendNotificationUnMarkedRsvpRequest: SendNotificationUnMarkedRsvpRequest,
    ): CustomResponse<Void>
}
