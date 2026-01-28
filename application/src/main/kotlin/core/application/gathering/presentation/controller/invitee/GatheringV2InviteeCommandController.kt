package core.application.gathering.presentation.controller.invitee

import core.application.common.exception.CustomResponse
import core.application.gathering.presentation.request.SubmitGatheringV2RsvpStatusRequest
import core.application.security.annotation.CurrentMemberId
import core.domain.gathering.port.inbound.GatheringV2InviteeCommandUseCase
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.vo.MemberId
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/gatherings")
class GatheringV2InviteeCommandController(
    val gatheringV2InviteeCommandUseCase: GatheringV2InviteeCommandUseCase,
) : GatheringV2InviteeCommandApi {
    @PostMapping("/{gatheringId}/rsvp-status")
    override fun submitGatheringV2RsvpStatus(
        @CurrentMemberId
        memberId: MemberId,
        @PathVariable("gatheringId")
        gatheringV2Id: GatheringV2Id,
        @RequestBody submitGatheringV2RsvpStatusRequest: SubmitGatheringV2RsvpStatusRequest,
    ): CustomResponse<Void> {
        gatheringV2InviteeCommandUseCase.submitGatheringV2RsvpStatus(
            memberId = memberId,
            gatheringV2Id = gatheringV2Id,
            isRsvpGoing = submitGatheringV2RsvpStatusRequest.isRsvpGoing,
        )
        return CustomResponse.ok()
    }
}
