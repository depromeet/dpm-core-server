package core.application.afterParty.presentation.controller.invitee

import core.application.afterParty.presentation.request.SubmitAfterPartyRsvpStatusRequest
import core.application.common.exception.CustomResponse
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "AfterParty", description = "회식 참여 조사 API")
interface AfterPartyInviteeCommandApi {
    @ApiResponse(
        responseCode = "200",
        description = "회식 참여 여부 제출 성공",
    )
    @Operation(
        summary = "회식 참여 여부 제출 API",
        description = "회식 참여 여부를 제출하는 API입니다",
    )
    fun submitAfterPartyRsvpStatus(
        memberId: MemberId,
        afterPartyId: AfterPartyId,
        submitAfterPartyRsvpStatusRequest: SubmitAfterPartyRsvpStatusRequest,
    ): CustomResponse<Void>
}
