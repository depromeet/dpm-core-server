package core.application.gathering.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.gathering.presentation.response.GatheringV2InviteTagListResponse
import core.application.gathering.presentation.response.GatheringV2ListResponse
import core.domain.gathering.port.inbound.GatheringV2QueryUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/gatherings")
class GatheringV2QueryController(
    val gatheringV2QueryUseCase: GatheringV2QueryUseCase,
) : GatheringV2QueryApi {
    @GetMapping("/invite-tags")
    override fun getGatheringV2InviteTagList(): CustomResponse<GatheringV2InviteTagListResponse> =
        CustomResponse.ok(
            GatheringV2InviteTagListResponse(),
        )

    @GetMapping
    override fun getGatheringV2List(): CustomResponse<GatheringV2ListResponse> {
        val retrievedGatherings = gatheringV2QueryUseCase.getAllGatherings()

        return CustomResponse.ok()
    }
}
