package core.application.afterParty.presentation.controller.invitee

import core.application.afterParty.application.service.invitee.AfterPartyInviteeQueryService
import core.application.afterParty.presentation.response.AfterPartyRsvpMemberResponse
import core.application.common.exception.CustomResponse
import core.domain.afterParty.vo.AfterPartyId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/after-party")
class AfterPartyInviteeQueryController(
    val afterPartyInviteeQueryService: AfterPartyInviteeQueryService,
) : AfterPartyInviteeQueryApi {
    @PreAuthorize("hasAuthority('read:after_party')")
    @GetMapping("/{afterPartyId}/rsvp-members")
    override fun getAfterPartyRsvpMemberList(
        @PathVariable("afterPartyId") afterPartyId: AfterPartyId,
    ): CustomResponse<List<AfterPartyRsvpMemberResponse>> =
        CustomResponse.ok(afterPartyInviteeQueryService.getRsvpMembers(afterPartyId))
}
