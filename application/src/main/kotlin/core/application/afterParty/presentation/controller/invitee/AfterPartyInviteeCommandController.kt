package core.application.afterParty.presentation.controller.invitee

import core.application.afterParty.presentation.request.SubmitAfterPartyRsvpStatusRequest
import core.application.common.exception.CustomResponse
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
@RequestMapping("/v2/after-parties")
class AfterPartyInviteeCommandController(
    val afterPartyInviteeCommandUseCase: AfterPartyInviteeCommandUseCase,
) : AfterPartyInviteeCommandApi {
    @PreAuthorize("hasAuthority('update:after_party')")
    @PostMapping("/{afterPartyId}/rsvp-status")
    override fun submitAfterPartyRsvpStatus(
        @CurrentMemberId
        memberId: MemberId,
        @PathVariable("afterPartyId")
        afterPartyId: AfterPartyId,
        @RequestBody submitAfterPartyRsvpStatusRequest: SubmitAfterPartyRsvpStatusRequest,
    ): CustomResponse<Void> {
        afterPartyInviteeCommandUseCase.submitAfterPartyRsvpStatus(
            memberId = memberId,
            afterPartyId = afterPartyId,
            isRsvpGoing = submitAfterPartyRsvpStatusRequest.isRsvpGoing,
        )
        return CustomResponse.ok()
    }
}
