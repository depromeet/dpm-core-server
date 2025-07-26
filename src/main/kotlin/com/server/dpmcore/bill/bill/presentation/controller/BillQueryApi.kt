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
                                  "code": "G000",
                                  "data": {
                                    "title": "17기 OT세션 공식 회식",
                                    "description": "OT 이후 첫 공식 회식 자리입니다. 운영진 및 디퍼 전체가 초대됩니다.",
                                    "hostUserId": 1,
                                    "billTotalAmount": 100,
                                    "billStatus": "OPEN",
                                    "createdAt": "2025-07-22T22:07:30.968754",
                                    "billAccountId": 1,
                                    "gatherings": [
                                      {
                                        "title": "1차 회식 - 고깃집",
                                        "description": "가까운 삼겹살 집에서 진행",
                                        "roundNumber": 1,
                                        "heldAt": "2025-07-22T05:55:34.606",
                                        "category": "GATHERING",
                                        "joinMemberCount": 0,
                                        "amount": 820000,
                                        "gatheringMembers": [
                                          {
                                            "memberId": 1,
                                            "isJoined": false,
                                            "isCompleted": false
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
                                  "code": "G000",
                                  "data": {
                                    "bills": [
                                      {
                                        "billId": 1,
                                        "title": "17기 OT세션 공식 회식 111",
                                        "description": "OT 이후 첫 공식 회식 자리입니다. 운영진 및 디퍼 전체가 초대됩니다.",
                                        "billTotalAmount": 0,
                                        "billStatus": "IN_PROGRESS",
                                        "createdAt": "2025-07-23T22:50:21.466944",
                                        "billAccountId": 1,
                                        "inviteGroups": null,
                                        "answerMemberCount": 0,
                                        "gatherings": []
                                      },
                                      {
                                        "billId": 2,
                                        "title": "17기 OT세션 공식 회식 222",
                                        "description": "OT 이후 첫 공식 회식 자리입니다. 운영진 및 디퍼 전체가 초대됩니다.",
                                        "billTotalAmount": 820000,
                                        "billStatus": "IN_PROGRESS",
                                        "createdAt": "2025-07-23T22:50:28.643061",
                                        "billAccountId": 1,
                                        "inviteGroups": null,
                                        "answerMemberCount": 0,
                                        "gatherings": [
                                          {
                                            "title": "1차 회식 - 고깃집",
                                            "description": "가까운 삼겹살 집에서 진행",
                                            "roundNumber": 1,
                                            "heldAt": "2025-07-22T05:55:34.606",
                                            "category": "GATHERING",
                                            "receipt": null,
                                            "joinMemberCount": 1,
                                            "amount": 820000,
                                            "splitAmount": 820000
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
                          "code": "G000",
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
