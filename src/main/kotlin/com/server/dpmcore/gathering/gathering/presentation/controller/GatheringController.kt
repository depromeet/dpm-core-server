package com.server.dpmcore.gathering.gathering.presentation.controller

import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.gathering.gathering.application.GatheringQueryService
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gathering.presentation.response.GatheringMemberJoinListResponse
import jakarta.validation.constraints.Positive
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/gatherings")
class GatheringController(
    private val gatheringQueryService: GatheringQueryService,
) : GatheringApi {
    @GetMapping("/{gatheringId}")
    override fun getGatheringMemberJoinList(
        @Positive @PathVariable gatheringId: GatheringId,
    ): CustomResponse<GatheringMemberJoinListResponse> {
        val response = gatheringQueryService.getGatheringMemberJoinList(gatheringId)
        return CustomResponse.ok(response)
    }
}
