package core.application.gathering.presentation.controller

import core.application.common.converter.TimeMapper.localDateTimeToInstant
import core.application.common.exception.CustomResponse
import core.application.gathering.presentation.request.CreateGatheringV2ByInviteTagNamesRequest
import core.application.gathering.presentation.request.CreateGatheringV2Request
import core.application.gathering.presentation.request.UpdateGatheringV2Request
import core.application.security.annotation.CurrentMemberId
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.enums.GatheringCategory
import core.domain.gathering.enums.GatheringV2InviteTag
import core.domain.gathering.port.inbound.GatheringV2CommandUseCase
import core.domain.gathering.vo.GatheringV2Id
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
    val gatheringV2CommandUseCase: GatheringV2CommandUseCase,
) : GatheringV2CommandApi {
    @PreAuthorize("hasAuthority('create:gathering')")
    @PostMapping
    override fun createGatheringV2(
        @RequestBody createGatheringV2Request: CreateGatheringV2Request,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<Void> {
        val gatheringV2InviteTags: List<GatheringV2InviteTag> =
            createGatheringV2Request.inviteTags.map { it.toDomain() }
        val createGatheringV2: GatheringV2 =
            GatheringV2.create(
                title = createGatheringV2Request.title,
                description = createGatheringV2Request.description,
                category = GatheringCategory.GATHERING,
                scheduledAt = localDateTimeToInstant(createGatheringV2Request.scheduledAt),
                closedAt = localDateTimeToInstant(createGatheringV2Request.closedAt),
                authorMemberId = memberId,
                canEditAfterApproval = createGatheringV2Request.canEditAfterApproval,
            )
        gatheringV2CommandUseCase.createGatheringV2(
            gatheringV2 = createGatheringV2,
            gatheringV2InviteTags = gatheringV2InviteTags,
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
        val createGatheringV2: GatheringV2 =
            GatheringV2.create(
                title = createGatheringV2ByInviteTagNamesRequest.title,
                description = createGatheringV2ByInviteTagNamesRequest.description,
                category = GatheringCategory.GATHERING,
                scheduledAt = localDateTimeToInstant(createGatheringV2ByInviteTagNamesRequest.scheduledAt),
                closedAt = localDateTimeToInstant(createGatheringV2ByInviteTagNamesRequest.closedAt),
                authorMemberId = memberId,
                canEditAfterApproval = createGatheringV2ByInviteTagNamesRequest.canEditAfterApproval,
            )
        gatheringV2CommandUseCase.createGatheringV2ByInviteTagNames(
            gatheringV2 = createGatheringV2,
            inviteTagNames = createGatheringV2ByInviteTagNamesRequest.inviteTagNames,
            authorMemberId = memberId,
        )
        return CustomResponse.ok()
    }

    @PreAuthorize("hasAuthority('create:gathering')")
    @PatchMapping("/{gatheringId}")
    override fun updateGatheringV2(
        @RequestBody updateGatheringV2Request: UpdateGatheringV2Request,
        @CurrentMemberId memberId: MemberId,
        @PathVariable gatheringId: GatheringV2Id,
    ): CustomResponse<Void> {
        val gatheringV2InviteTags: List<GatheringV2InviteTag> =
            updateGatheringV2Request.inviteTags.map { it.toDomain() }
        val updateGatheringV2: GatheringV2 = updateGatheringV2Request.toDomain(gatheringId)
        gatheringV2CommandUseCase.updateGatheringV2(
            gatheringV2 = updateGatheringV2,
        )
        return CustomResponse.ok()
    }
}
