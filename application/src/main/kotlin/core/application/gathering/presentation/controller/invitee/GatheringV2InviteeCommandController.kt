package core.application.gathering.presentation.controller.invitee

import core.application.common.exception.CustomResponse
import core.application.gathering.presentation.request.SubmitGatheringV2RsvpStatusRequest
import core.application.security.annotation.CurrentMemberId
import core.domain.afterParty.port.inbound.AfterPartyInviteeCommandUseCase
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.vo.MemberId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/gatherings")
class GatheringV2InviteeCommandController(
    val afterPartyInviteeCommandUseCase: AfterPartyInviteeCommandUseCase,
) : GatheringV2InviteeCommandApi {
    @PreAuthorize("hasAuthority('update:gathering')")
    @PostMapping("/{gatheringId}/rsvp-status")
    override fun submitGatheringV2RsvpStatus(
        @CurrentMemberId
        memberId: MemberId,
        @PathVariable("gatheringId")
        gatheringId: AfterPartyId,
        @RequestBody submitGatheringV2RsvpStatusRequest: SubmitGatheringV2RsvpStatusRequest,
    ): CustomResponse<Void> {
        afterPartyInviteeCommandUseCase.submitAfterPartyRsvpStatus(
            memberId = memberId,
            afterPartyId = gatheringId,
            isRsvpGoing = submitGatheringV2RsvpStatusRequest.isRsvpGoing,
        )
        return CustomResponse.ok()
    }
}
