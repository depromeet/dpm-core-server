package core.application.gathering.presentation.controller.invitee

import core.application.common.exception.CustomResponse
import core.application.gathering.presentation.response.GatheringV2InviteTagListResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Tag(name = "GatheringV2", description = "회식 참여 조사 API")
interface GatheringV2InviteeQueryApi {
    @ApiResponse(
        responseCode = "200",
        description = "회식 초대자 목록",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "회식 초대자 목록 조회 성공 응답",
                        value = """
                            {
                              "status": "OK",
                              "message": "요청에 성공했습니다",
                              "code": "GLOBAL-200-01",
                              "data": {
                                "inviteTags": [
                                  {
                                    "cohortId": 1,
                                    "authorityId": 1,
                                    "tagName": "17기 디퍼"
                                  },
                                  {
                                    "cohortId": 1,
                                    "authorityId": 2,
                                    "tagName": "17기 운영진"
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
        summary = "회식 초대자 목록 조회 API",
        description = "회식에 초대한 유저 목록을 조회합니다",
    )
    fun getGatheringV2InviteMemberList(): CustomResponse<GatheringV2InviteTagListResponse>
}
