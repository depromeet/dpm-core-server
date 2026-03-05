package core.application.gathering.presentation.controller

import core.application.common.converter.TimeMapper.localDateTimeToInstant
import core.application.common.exception.CustomResponse
import core.application.gathering.presentation.request.CreateGatheringV2ByInviteTagNamesRequest
import core.application.gathering.presentation.request.CreateGatheringV2Request
import core.application.gathering.presentation.request.UpdateGatheringV2Request
import core.application.security.annotation.CurrentMemberId
import core.domain.afterParty.aggregate.AfterParty
import core.domain.afterParty.enums.AfterPartyCategory
import core.domain.afterParty.enums.AfterPartyInviteTagEnum
import core.domain.afterParty.port.inbound.AfterPartyCommandUseCase
import core.domain.afterParty.vo.AfterPartyId
import core.domain.member.vo.MemberId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/gatherings")
class GatheringV2CommandController(
    val afterPartyCommandUseCase: AfterPartyCommandUseCase,
) : GatheringV2CommandApi {
    @PreAuthorize("hasAuthority('create:gathering')")
    @PostMapping
    override fun createGatheringV2(
        @RequestBody createGatheringV2Request: CreateGatheringV2Request,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<Void> {
        val afterPartyInviteTags: List<AfterPartyInviteTagEnum> =
            createGatheringV2Request.inviteTags.map { it.toDomain() }
        val createAfterParty: AfterParty =
            AfterParty.create(
                title = createGatheringV2Request.title,
                description = createGatheringV2Request.description,
                category = AfterPartyCategory.AFTER_PARTY,
                scheduledAt = localDateTimeToInstant(createGatheringV2Request.scheduledAt),
                closedAt = localDateTimeToInstant(createGatheringV2Request.closedAt),
                authorMemberId = memberId,
                canEditAfterApproval = createGatheringV2Request.canEditAfterApproval,
            )
        afterPartyCommandUseCase.createAfterParty(
            afterParty = createAfterParty,
            afterPartyInviteTags = afterPartyInviteTags,
            authorMemberId = memberId,
        )
        return CustomResponse.ok()
    }

    @PreAuthorize("hasAuthority('create:gathering')")
    @PostMapping("/by-invite-tag-names")
    override fun createGatheringV2ByInviteTagNames(
        @RequestBody createGatheringV2ByInviteTagNamesRequest: CreateGatheringV2ByInviteTagNamesRequest,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<Void> {
        val createAfterParty: AfterParty =
            AfterParty.create(
                title = createGatheringV2ByInviteTagNamesRequest.title,
                description = createGatheringV2ByInviteTagNamesRequest.description,
                category = AfterPartyCategory.AFTER_PARTY,
                scheduledAt = localDateTimeToInstant(createGatheringV2ByInviteTagNamesRequest.scheduledAt),
                closedAt = localDateTimeToInstant(createGatheringV2ByInviteTagNamesRequest.closedAt),
                authorMemberId = memberId,
                canEditAfterApproval = createGatheringV2ByInviteTagNamesRequest.canEditAfterApproval,
            )
        afterPartyCommandUseCase.createAfterPartyByInviteTagNames(
            afterParty = createAfterParty,
            inviteTagNames = createGatheringV2ByInviteTagNamesRequest.inviteTagNames,
            authorMemberId = memberId,
        )
        return CustomResponse.ok()
    }

    @PreAuthorize("hasAuthority('update:gathering')")
    @PatchMapping("/{gatheringId}")
    override fun updateGatheringV2(
        @RequestBody updateGatheringV2Request: UpdateGatheringV2Request,
        @CurrentMemberId memberId: MemberId,
        @PathVariable gatheringId: AfterPartyId,
    ): CustomResponse<Void> {
        val afterPartyInviteTags: List<AfterPartyInviteTagEnum> =
            updateGatheringV2Request.inviteTags.map { it.toDomain() }
        val updateAfterParty: AfterParty = updateGatheringV2Request.toDomain(gatheringId)
        afterPartyCommandUseCase.updateAfterParty(
            afterParty = updateAfterParty,
        )
        return CustomResponse.ok()
    }
}
