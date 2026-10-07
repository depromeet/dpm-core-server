package core.application.gathering.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.gathering.presentation.response.GatheringMemberJoinListResponse
import core.domain.gathering.vo.GatheringId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Positive
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Tag(name = "Gathering", description = "회식 API")
@Deprecated("AfterParty로 회식 API가 대체될 예정입니다.")
interface GatheringApi {
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
                                "name": "이영희",
                                "authority": "17_ORGANIZER",
                                "isJoined": true
                              },
                              {
                                "name": "김철수",
                                "authority": "17_ORGANIZER",
                                "isJoined": false
                              },
                              {
                                "name": "박민수",
                                "authority": "17_DEEPER",
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
