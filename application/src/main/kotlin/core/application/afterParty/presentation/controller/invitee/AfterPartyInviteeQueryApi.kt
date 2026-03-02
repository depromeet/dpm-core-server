package core.application.afterParty.presentation.controller.invitee

import core.application.afterParty.presentation.response.AfterPartyRsvpMemberResponse
import core.application.common.exception.CustomResponse
import core.domain.afterParty.vo.AfterPartyId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Tag(name = "AfterParty", description = "회식 참여 조사 API")
interface AfterPartyInviteeQueryApi {
    @ApiResponse(
        responseCode = "200",
        description = "회식 초대자 목록 조회",
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
                              "data": [
                                {
                                  "memberId": 1,
                                  "name": "준원카카오",
                                  "part": "SERVER",
                                  "team": 1,
                                  "isRsvpGoing": true
                                },
                                {
                                  "memberId": 2,
                                  "name": "신민철",
                                  "part": "SERVER",
                                  "team": 1,
                                  "isRsvpGoing": false
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
        summary = "회식 초대자 목록 조회 API",
        description = "회식 초대자 목록을 조회합니다",
    )
    fun getAfterPartyRsvpMemberList(afterPartyId: AfterPartyId): CustomResponse<List<AfterPartyRsvpMemberResponse>>
}
