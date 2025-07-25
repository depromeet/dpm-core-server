package com.server.dpmcore.gathering.gathering.presentation.controller

import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.gathering.gathering.application.GatheringCommandService
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.security.annotation.CurrentMemberId
import jakarta.validation.constraints.Positive
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/gatherings")
class GatheringController(
    private val gatheringCommandService: GatheringCommandService,
) : GatheringApi {
    @PatchMapping("/{gatheringId}/join")
    override fun markAsJoined(
        @Positive @PathVariable gatheringId: GatheringId,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<Void> {
        gatheringCommandService.markAsJoinedEachGatheringMember(gatheringId, memberId)
        return CustomResponse.noContent()
    }
}
