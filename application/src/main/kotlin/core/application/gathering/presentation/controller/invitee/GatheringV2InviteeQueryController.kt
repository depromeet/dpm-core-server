package core.application.gathering.presentation.controller.invitee

import core.application.common.exception.CustomResponse
import core.application.gathering.application.service.invitee.GatheringV2InviteeQueryService
import core.application.gathering.presentation.response.GatheringV2RsvpMemberResponse
import core.domain.gathering.vo.GatheringV2Id
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/gatherings")
class GatheringV2InviteeQueryController(
    val gatheringV2InviteeQueryService: GatheringV2InviteeQueryService,
) : GatheringV2InviteeQueryApi {
    @PreAuthorize("hasAuthority('read:gathering')")
    @GetMapping("/{gatheringId}/rsvp-members")
    override fun getGatheringV2RsvpMemberList(
        @PathVariable("gatheringId") gatheringV2Id: GatheringV2Id,
    ): CustomResponse<List<GatheringV2RsvpMemberResponse>> {
        return CustomResponse.ok(gatheringV2InviteeQueryService.getRsvpMembers(gatheringV2Id))
    }
}
