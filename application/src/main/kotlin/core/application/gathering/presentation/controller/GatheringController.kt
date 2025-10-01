package core.application.gathering.presentation.controller

import core.domain.gathering.vo.GatheringId
import core.application.common.exception.CustomResponse
import core.application.gathering.application.service.GatheringCommandService
import core.application.gathering.application.service.GatheringQueryService
import core.application.gathering.presentation.response.GatheringMemberJoinListResponse
import jakarta.validation.constraints.Positive
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/gatherings")
class GatheringController(
    private val gatheringCommandService: GatheringCommandService,
    private val gatheringQueryService: GatheringQueryService,
) : GatheringApi {
    @PreAuthorize("!hasRole('ROLE_GUEST')")
    @GetMapping("/{gatheringId}/participant-members")
    override fun getGatheringMemberJoinList(
        @Positive @PathVariable gatheringId: GatheringId,
    ): CustomResponse<GatheringMemberJoinListResponse> {
        val response = gatheringQueryService.getGatheringMemberJoinList(gatheringId)
        return CustomResponse.ok(response)
    }
}
