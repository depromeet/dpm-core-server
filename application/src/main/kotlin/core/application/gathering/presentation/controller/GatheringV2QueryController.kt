package core.application.gathering.presentation.controller

import core.application.afterParty.application.service.AfterPartyQueryService
import core.application.common.exception.CustomResponse
import core.application.gathering.presentation.response.GatheringV2DetailResponse
import core.application.gathering.presentation.response.GatheringV2InviteTagListResponse
import core.application.gathering.presentation.response.GatheringV2ListResponse
import core.application.security.annotation.CurrentMemberId
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.vo.MemberId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/gatherings")
class GatheringV2QueryController(
    val afterPartyQueryService: AfterPartyQueryService,
) : GatheringV2QueryApi {
    @PreAuthorize("hasAuthority('read:gathering')")
    @GetMapping("/invite-tags")
    override fun getGatheringV2InviteTagList(): CustomResponse<GatheringV2InviteTagListResponse> =
        CustomResponse.ok(
            afterPartyQueryService.getGatheringV2InviteTags(),
        )

    @PreAuthorize("hasAuthority('read:gathering')")
    @GetMapping
    override fun getGatheringV2List(
        @CurrentMemberId memberId: MemberId,
        @RequestParam(required = false) inviteTagCohortId: Long?,
        @RequestParam(required = false) inviteTagAuthorityId: Long?,
    ): CustomResponse<List<GatheringV2ListResponse>> =
        CustomResponse.ok(
            afterPartyQueryService.getAllGatherings(
                memberId = memberId,
                inviteTagCohortId = inviteTagCohortId,
                inviteTagAuthorityId = inviteTagAuthorityId,
            ),
        )

    @PreAuthorize("hasAuthority('read:gathering')")
    @GetMapping("/{gatheringId}")
    override fun getGatheringV2Detail(
        @PathVariable("gatheringId") afterPartyId: AfterPartyId,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<GatheringV2DetailResponse> =
        CustomResponse.ok(afterPartyQueryService.getGatheringV2Detail(afterPartyId, memberId))
}
