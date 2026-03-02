package core.application.afterParty.presentation.controller

import core.application.afterParty.presentation.request.CreateAfterPartyByInviteTagNamesRequest
import core.application.afterParty.presentation.request.CreateAfterPartyRequest
import core.application.afterParty.presentation.request.UpdateAfterPartyRequest
import core.application.common.converter.TimeMapper.localDateTimeToInstant
import core.application.common.exception.CustomResponse
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
@RequestMapping("/v2/after-party")
class AfterPartyCommandController(
    val afterPartyCommandUseCase: AfterPartyCommandUseCase,
) : AfterPartyCommandApi {
    @PreAuthorize("hasAuthority('create:after_party')")
    @PostMapping
    override fun createAfterParty(
        @RequestBody createAfterPartyRequest: CreateAfterPartyRequest,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<Void> {
        val afterPartyInviteTags: List<AfterPartyInviteTagEnum> =
            createAfterPartyRequest.inviteTags.map { it.toDomain() }
        val createAfterParty: AfterParty =
            AfterParty.create(
                title = createAfterPartyRequest.title,
                description = createAfterPartyRequest.description,
                category = AfterPartyCategory.AFTER_PARTY,
                scheduledAt = localDateTimeToInstant(createAfterPartyRequest.scheduledAt),
                closedAt = localDateTimeToInstant(createAfterPartyRequest.closedAt),
                authorMemberId = memberId,
                canEditAfterApproval = createAfterPartyRequest.canEditAfterApproval,
            )
        afterPartyCommandUseCase.createAfterParty(
            afterParty = createAfterParty,
            afterPartyInviteTags = afterPartyInviteTags,
            authorMemberId = memberId,
        )
        return CustomResponse.ok()
    }

    @PreAuthorize("hasAuthority('create:after_party')")
    @PostMapping("/by-invite-tag-names")
    override fun createAfterPartyByInviteTagNames(
        @RequestBody createAfterPartyByInviteTagNamesRequest: CreateAfterPartyByInviteTagNamesRequest,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<Void> {
        val createAfterParty: AfterParty =
            AfterParty.create(
                title = createAfterPartyByInviteTagNamesRequest.title,
                description = createAfterPartyByInviteTagNamesRequest.description,
                category = AfterPartyCategory.AFTER_PARTY,
                scheduledAt = localDateTimeToInstant(createAfterPartyByInviteTagNamesRequest.scheduledAt),
                closedAt = localDateTimeToInstant(createAfterPartyByInviteTagNamesRequest.closedAt),
                authorMemberId = memberId,
                canEditAfterApproval = createAfterPartyByInviteTagNamesRequest.canEditAfterApproval,
            )
        afterPartyCommandUseCase.createAfterPartyByInviteTagNames(
            afterParty = createAfterParty,
            inviteTagNames = createAfterPartyByInviteTagNamesRequest.inviteTagNames,
            authorMemberId = memberId,
        )
        return CustomResponse.ok()
    }

    @PreAuthorize("hasAuthority('update:after_party')")
    @PatchMapping("/{afterPartyId}")
    override fun updateAfterParty(
        @RequestBody updateAfterPartyRequest: UpdateAfterPartyRequest,
        @CurrentMemberId memberId: MemberId,
        @PathVariable afterPartyId: AfterPartyId,
    ): CustomResponse<Void> {
        val afterPartyInviteTags: List<AfterPartyInviteTagEnum> =
            updateAfterPartyRequest.inviteTags.map { it.toDomain() }
        val updateAfterParty: AfterParty = updateAfterPartyRequest.toDomain(afterPartyId)
        afterPartyCommandUseCase.updateAfterParty(
            afterParty = updateAfterParty,
        )
        return CustomResponse.ok()
    }
}
