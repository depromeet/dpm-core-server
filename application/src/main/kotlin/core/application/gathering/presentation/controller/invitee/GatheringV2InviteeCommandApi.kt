package core.application.gathering.presentation.controller.invitee

import core.application.common.exception.CustomResponse
import core.application.gathering.presentation.request.SubmitGatheringV2RsvpStatusRequest
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "GatheringV2", description = "회식 참여 조사 API")
interface GatheringV2InviteeCommandApi {
    @ApiResponse(
        responseCode = "200",
        description = "회식 참여 여부 제출 성공",
    )
    @Operation(
        summary = "회식 참여 여부 제출 API",
        description = "회식 참여 여부를 제출하는 API입니다",
    )
    fun submitGatheringV2RsvpStatus(
        memberId: MemberId,
        gatheringV2Id: GatheringV2Id,
        submitGatheringV2RsvpStatusRequest: SubmitGatheringV2RsvpStatusRequest,
    ): CustomResponse<Void>
}
