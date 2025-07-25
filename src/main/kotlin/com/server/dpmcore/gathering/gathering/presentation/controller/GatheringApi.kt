package com.server.dpmcore.gathering.gathering.presentation.controller

import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.member.member.domain.model.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Positive
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Tag(name = "Gathering", description = "회식 API")
interface GatheringApi {
    @ApiResponse(
        responseCode = "204",
        description = "단일 회식 참여 추가",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
            ),
        ],
    )
    @Operation(
        summary = "단일 회식 참여 추가",
        description = "특정 단일 회식에 참여 하였음으로 표시합니다.",
    )
    fun markAsJoined(
        @Positive gatheringId: GatheringId,
        memberId: MemberId,
    ): CustomResponse<Void>
}
