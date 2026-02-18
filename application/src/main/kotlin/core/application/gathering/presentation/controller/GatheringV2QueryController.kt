package core.application.gathering.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.gathering.application.service.GatheringV2QueryService
import core.application.gathering.presentation.response.GatheringV2DetailResponse
import core.application.gathering.presentation.response.GatheringV2InviteTagListResponse
import core.application.gathering.presentation.response.GatheringV2ListResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.gathering.vo.GatheringV2Id
import core.domain.member.vo.MemberId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/gatherings")
class GatheringV2QueryController(
    val gatheringV2QueryService: GatheringV2QueryService,
) : GatheringV2QueryApi {
    @GetMapping("/invite-tags")
    override fun getGatheringV2InviteTagList(): CustomResponse<GatheringV2InviteTagListResponse> =
        CustomResponse.ok(
            gatheringV2QueryService.getGatheringV2InviteTags(),
        )

    @GetMapping
    override fun getGatheringV2List(
        @CurrentMemberId memberId: MemberId,
        @RequestParam(required = false) inviteTagCohortId: Long?,
        @RequestParam(required = false) inviteTagAuthorityId: Long?,
    ): CustomResponse<List<GatheringV2ListResponse>> {
        return CustomResponse.ok(
            gatheringV2QueryService.getAllGatherings(
                memberId = memberId,
                inviteTagCohortId = inviteTagCohortId,
                inviteTagAuthorityId = inviteTagAuthorityId,
            ),
        )
    }

    @GetMapping("/{gatheringId}")
    override fun getGatheringV2Detail(
        @PathVariable("gatheringId") gatheringV2Id: GatheringV2Id,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<GatheringV2DetailResponse> =
        CustomResponse.ok(gatheringV2QueryService.getGatheringV2Detail(gatheringV2Id, memberId))
}
