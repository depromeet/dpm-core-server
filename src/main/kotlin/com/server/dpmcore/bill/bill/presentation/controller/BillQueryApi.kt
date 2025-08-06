package com.server.dpmcore.bill.bill.presentation.controller

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.presentation.dto.response.BillDetailResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillListResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillSummaryListByMemberResponse
import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.member.member.domain.model.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Positive
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Tag(name = "Bill", description = "정산 API")
interface BillQueryApi {
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정산서 조회 성공",
                content = [
                    Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "정산서 조회 성공 응답",
                                value = """
                                    {
                                      "status": "OK",
                                      "message": "요청에 성공했습니다",
                                      "code": "GLOBAL-200-01",
                                      "data": {
                                        "billId": 1,
                                        "title": "1차 회식 정산",
                                        "description": "1차 회식에 대한 정산입니다.",
                                        "hostUserId": 17,
                                        "billTotalAmount": 220000,
                                        "billTotalSplitAmount": 170000,
                                        "billStatus": "IN_PROGRESS",
                                        "createdAt": "2024-01-01T19:00:00",
                                        "billAccountId": 1,
                                        "invitedMemberCount": 2,
                                        "invitationConfirmedCount": 0,
                                        "invitationCheckedMemberCount": 1,
                                        "inviteAuthorities": [
                                          {
                                            "inviteAuthorityId": 1,
                                            "authorityName": "DEEPER",
                                            "authorityMemberCount": 64
                                          },
                                          {
                                            "inviteAuthorityId": 2,
                                            "authorityName": "ORGANIZER",
                                            "authorityMemberCount": 12
                                          }
                                        ],
                                        "gatherings": [
                                          {
                                            "gatheringId": 1,
                                            "title": "1차 회식",
                                            "description": "첫 번째 회식입니다.",
                                            "roundNumber": 1,
                                            "heldAt": "2024-01-06T04:00:00",
                                            "category": "GATHERING",
                                            "joinMemberCount": 0,
                                            "amount": 100000
                                          },
                                          {
                                            "gatheringId": 2,
                                            "title": "2차 회식",
                                            "description": "두 번째 회식입니다.",
                                            "roundNumber": 2,
                                            "heldAt": "2024-01-06T06:00:00",
                                            "category": "GATHERING",
                                            "joinMemberCount": 0,
                                            "amount": 120000
                                          }
                                        ]
                                      }
                                    }
                            """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @Operation(
        summary = "정산서 조회 API",
        description =
            "정산서의 상세 정보를 조회합니다. \n" +
                "정산서에는 제목, 설명, 호스트 유저 ID, 총 금액, 생성일시, 정산 계좌 ID, 회식 차수 정보 등이 포함됩니다.",
    )
    fun getBillDetails(
        @Positive billId: BillId,
    ): CustomResponse<BillDetailResponse>

    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정산서 목록 조회 성공",
                content = [
                    Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "정산서 목록 조회 성공 응답",
                                value = """
                                    {
                                      "status": "OK",
                                      "message": "요청에 성공했습니다",
                                      "code": "GLOBAL-200-01",
                                      "data": {
                                        "bills": [
                                          {
                                            "billId": 1,
                                            "title": "1차 회식 정산",
                                            "description": "1차 회식에 대한 정산입니다.",
                                            "billTotalAmount": 220000,
                                            "billStatus": "IN_PROGRESS",
                                            "createdAt": "2024-01-01T19:00:00",
                                            "billAccountId": 1,
                                            "invitedMemberCount": 2,
                                            "invitationConfirmedCount": 0,
                                            "invitationCheckedMemberCount": 1,
                                            "inviteAuthorities": [
                                              {
                                                "invitedAuthorityId": 1,
                                                "authorityName": "DEEPER",
                                                "authorityMemberCount": 64
                                              },
                                              {
                                                "invitedAuthorityId": 2,
                                                "authorityName": "ORGANIZER",
                                                "authorityMemberCount": 12
                                              }
                                            ],
                                            "gatherings": [
                                              {
                                                "gatheringId": 1,
                                                "title": "1차 회식",
                                                "description": "첫 번째 회식입니다.",
                                                "roundNumber": 1,
                                                "heldAt": "2024-01-06T04:00:00",
                                                "category": "GATHERING",
                                                "receipt": null,
                                                "joinMemberCount": 2,
                                                "amount": 100000,
                                                "splitAmount": 50000
                                              },
                                              {
                                                "gatheringId": 2,
                                                "title": "2차 회식",
                                                "description": "두 번째 회식입니다.",
                                                "roundNumber": 2,
                                                "heldAt": "2024-01-06T06:00:00",
                                                "category": "GATHERING",
                                                "receipt": null,
                                                "joinMemberCount": 1,
                                                "amount": 120000,
                                                "splitAmount": 120000
                                              }
                                            ]
                                          }
                                        ]
                                      }
                                    }

                            """,
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @Operation(
        summary = "정산 목록 조회 API",
        description =
            "정산의 목록을 조회합니다. \n" +
                "정산에는 제목, 설명, 호스트 유저 ID, 총 금액, 생성일시, 정산 계좌 ID, 회식 차수 정보 등이 포함됩니다.",
    )
    fun getBillListByMemberId(memberId: MemberId): CustomResponse<BillListResponse>

    @ApiResponse(
        responseCode = "200",
        description = "정산서 멤버별 최종 금액 조회 성공",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
                examples = [
                    ExampleObject(
                        name = "정산서 멤버별 최종 금액 조회 성공 응답",
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
                                "splitAmount": 25000
                              },
                              {
                                "name": "신민철",
                                "authority": "ORGANIZER",
                                "splitAmount": 18000
                              },
                              {
                                "name": "정준원",
                                "authority": "DEEPER",
                                "splitAmount": 12000
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
        summary = "정산서 멤버별 최종 금액 조회 API",
        description = "정산서에 참여한 멤버들의 최종 금액을 목록 조회합니다.",
    )
    fun getMemberBillSummaries(
        @Positive billId: BillId,
    ): CustomResponse<BillSummaryListByMemberResponse>
}
