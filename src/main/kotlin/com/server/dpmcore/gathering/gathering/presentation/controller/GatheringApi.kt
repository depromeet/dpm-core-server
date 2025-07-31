package com.server.dpmcore.gathering.gathering.presentation.controller

import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.gathering.gathering.domain.model.GatheringId
import com.server.dpmcore.gathering.gathering.presentation.request.UpdateGatheringJoinsRequest
import com.server.dpmcore.gathering.gathering.presentation.response.GatheringMemberJoinListResponse
import com.server.dpmcore.member.member.domain.model.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Positive
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Tag(name = "Gathering", description = "회식 API")
interface GatheringApi {
    @ApiResponse(
        responseCode = "204",
        description = "각 회식 참여 여부 저장",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "각 회식 참여 여부 저장",
                        value = """
                                {
                                  "gatheringJoins": [
                                    {
                                      "gatheringId": 1,
                                      "isJoined": true
                                    },
                                    {
                                      "gatheringId": 2,
                                      "isJoined": false
                                    }
                                  ]
                                }
                            """,
                    ),
                ],
            ),
        ],
    )
    @Operation(
        summary = "각 회식 참여 여부 저장",
        description = "여러 회식의 참여 여부를 표시합니다.",
    )
    fun markAsJoined(
        request: UpdateGatheringJoinsRequest,
        memberId: MemberId,
    ): CustomResponse<Void>

    @ApiResponse(
        responseCode = "200",
        description = "회식별 멤버 참여 여부 조회 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "회식별 멤버 참여 여부 조회 성공 응답",
                        value = """
                        {
                          "status": "OK",
                          "message": "요청에 성공했습니다",
                          "code": "GLOBAL-200-01",
                          "data": {
                              "members": [
                              {
                                "name": "이한음",
                                "authority": "ORGANIZER",
                                "isJoined": true
                              },
                              {
                                "name": "신민철",
                                "authority": "ORGANIZER",
                                "isJoined": false
                              },
                              {
                                "name": "정준원",
                                "authority": "DEEPER",
                                "part": "SERVER",
                                "isJoined": true
                              }
                            ]
                          }
                        }
                    """,
                    ),
                ],
            ),
        ],
    )
    @Operation(
        summary = "회식별 멤버 참여 여부 조회 API",
        description = "정산서에서 회식 별로 초대된 멤버의 참여 여부를 목록 조회합니다",
    )
    fun getGatheringMemberJoinList(
        @Positive gatheringId: GatheringId,
    ): CustomResponse<GatheringMemberJoinListResponse>
}
