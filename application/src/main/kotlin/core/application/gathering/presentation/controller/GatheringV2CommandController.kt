package core.application.gathering.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.gathering.application.service.GatheringV2CommandService
import core.application.gathering.presentation.request.CreateGatheringV2Request
import core.application.session.presentation.mapper.TimeMapper.localDateTimeToInstant
import core.domain.gathering.aggregate.GatheringV2
import core.domain.gathering.enums.GatheringCategory
import core.domain.gathering.enums.GatheringV2InviteTag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/gatherings")
class GatheringV2CommandController(
    val gatheringV2CommandService: GatheringV2CommandService,
) : GatheringV2CommandApi {
    @PostMapping
    override fun createGatheringV2(
        @RequestBody createGatheringV2Request: CreateGatheringV2Request,
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
                canEditAfterApproval = createGatheringV2Request.canEditAfterApproval,
            )
        gatheringV2CommandService.createGatheringV2(
            createGatheringV2,
            gatheringV2InviteTags,
        )
        return CustomResponse.ok()
    }
}
