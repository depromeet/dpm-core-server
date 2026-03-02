package core.application.gathering.presentation.controller.invitee

import core.application.afterParty.application.service.invitee.AfterPartyInviteeQueryService
import core.application.common.exception.CustomResponse
import core.application.gathering.presentation.response.GatheringV2RsvpMemberResponse
import core.domain.afterParty.vo.AfterPartyId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/gatherings")
class GatheringV2InviteeQueryController(
    val gatheringV2InviteeQueryService: AfterPartyInviteeQueryService,
) : GatheringV2InviteeQueryApi {
    @PreAuthorize("hasAuthority('read:gathering')")
    @GetMapping("/{gatheringId}/rsvp-members")
    override fun getGatheringV2RsvpMemberList(
        @PathVariable("gatheringId") afterPartyId: AfterPartyId,
    ): CustomResponse<List<GatheringV2RsvpMemberResponse>> =
        CustomResponse.ok(gatheringV2InviteeQueryService.getRsvpMembersOld(afterPartyId))
}
