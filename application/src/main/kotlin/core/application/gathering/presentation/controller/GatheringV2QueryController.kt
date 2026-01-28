package core.application.gathering.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.gathering.application.service.GatheringV2QueryService
import core.application.gathering.presentation.response.GatheringV2DetailResponse
import core.application.gathering.presentation.response.GatheringV2InviteTagListResponse
import core.application.gathering.presentation.response.GatheringV2ListResponse
import core.application.gathering.presentation.response.GatheringV2RsvpMemberResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.vo.MemberId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/gatherings")
class GatheringV2QueryController(
    val gatheringV2QueryService: GatheringV2QueryService,
) : GatheringV2QueryApi {
    @GetMapping("/invite-tags")
    override fun getGatheringV2InviteTagList(): CustomResponse<GatheringV2InviteTagListResponse> =
        CustomResponse.ok(
            GatheringV2InviteTagListResponse(),
        )

    @GetMapping
    override fun getGatheringV2List(
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<List<GatheringV2ListResponse>> {
        return CustomResponse.ok(gatheringV2QueryService.getAllGatherings(memberId))
    }

    @GetMapping("/{gatheringId}/rsvp-members")
    override fun getGatheringV2RsvpMemberList(
        @PathVariable("gatheringId") gatheringV2Id: GatheringV2Id,
    ): CustomResponse<List<GatheringV2RsvpMemberResponse>> {
        return CustomResponse.ok(gatheringV2QueryService.getRsvpMembers(gatheringV2Id))
    }

    @GetMapping("/v2/gatherings/{gatheringId}")
    override fun getGatheringV2Detail(
        @PathVariable("gatheringId") gatheringV2Id: GatheringV2Id,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<GatheringV2DetailResponse> =
        CustomResponse.ok(gatheringV2QueryService.getGatheringV2Detail(gatheringV2Id, memberId))
}
