package core.application.afterParty.presentation.controller

import core.application.afterParty.application.service.AfterPartyQueryService
import core.application.afterParty.presentation.response.AfterPartyDetailResponse
import core.application.afterParty.presentation.response.AfterPartyInviteTagListResponse
import core.application.afterParty.presentation.response.AfterPartyListResponse
import core.application.common.exception.CustomResponse
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
@RequestMapping("/v2/after-parties")
class AfterPartyQueryController(
    val afterPartyQueryService: AfterPartyQueryService,
) : AfterPartyQueryApi {
    @PreAuthorize("hasAuthority('read:after_party')")
    @GetMapping("/invite-tags")
    override fun getAfterPartyInviteTagList(): CustomResponse<AfterPartyInviteTagListResponse> =
        CustomResponse.ok(
            afterPartyQueryService.getAfterPartyInviteTags(),
        )

    @PreAuthorize("hasAuthority('read:after_party')")
    @GetMapping
    override fun getAfterPartyList(
        @CurrentMemberId memberId: MemberId,
        @RequestParam(required = false) inviteTagCohortId: Long?,
        @RequestParam(required = false) inviteTagAuthorityId: Long?,
    ): CustomResponse<List<AfterPartyListResponse>> =
        CustomResponse.ok(
            afterPartyQueryService.getAllAfterPartys(
                memberId = memberId,
                inviteTagCohortId = inviteTagCohortId,
                inviteTagAuthorityId = inviteTagAuthorityId,
            ),
        )

    @PreAuthorize("hasAuthority('read:after_party')")
    @GetMapping("/{afterPartyId}")
    override fun getAfterPartyDetail(
        @PathVariable("afterPartyId") afterPartyId: AfterPartyId,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<AfterPartyDetailResponse> =
        CustomResponse.ok(afterPartyQueryService.getAfterPartyDetail(afterPartyId, memberId))
}
