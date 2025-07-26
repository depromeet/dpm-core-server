package com.server.dpmcore.gathering.gathering.presentation.controller

import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.gathering.gathering.application.GatheringCommandService
import com.server.dpmcore.gathering.gathering.presentation.request.UpdateGatheringJoinsRequest
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.security.annotation.CurrentMemberId
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/gatherings")
class GatheringController(
    private val gatheringCommandService: GatheringCommandService,
) : GatheringApi {
    @PatchMapping("/{gatheringId}/join")
    override fun markAsJoined(
        @RequestBody request: UpdateGatheringJoinsRequest,
        @CurrentMemberId memberId: MemberId,
    ): CustomResponse<Void> {
        gatheringCommandService.markAsJoinedEachGatheringMember(request, memberId)
        return CustomResponse.noContent()
    }
}
