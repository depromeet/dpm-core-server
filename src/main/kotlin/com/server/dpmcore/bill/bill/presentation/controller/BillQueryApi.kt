package com.server.dpmcore.bill.bill.presentation.controller

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.common.exception.CustomResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "정산(Bill)")
interface BillQueryApi {
    @Operation(
        summary = "정산 상세 조회",
        description =
            "정산 상세 정보를 조회합니다. \n" +
                "추후에는 영수증 사진이 추가될 수 있습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정산 상세 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "정산 상세 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공하였습니다.",
                                        "code": "BILL-001",
                                        "data": {
                                            "billId": 1,
                                            "title": "17기 OT세션 공식 회식",
                                            "description": "OT 이후 첫 공식 회식 자리입니다. 운영진 및 디퍼 전체가 초대됩니다.",
                                            "billTotalAmount": 1240000,
                                            "heldAt": "2025-07-05T09:00:00",
                                            "billAccountId": 2,
                                            "inviteGroups": [
                                                {
                                                    "inviteGroupId": 1,
                                                    "groupName": "17기 운영진",
                                                    "groupMemberCount": 10
                                                },
                                                {
                                                    "inviteGroupId": 2,
                                                    "groupName": "17기 디퍼",
                                                    "groupMemberCount": 60
                                                }
                                            ],
                                            "gatherings": [
                                                {
                                                    "title": "1차 회식 - 고깃집",
                                                    "description": "가까운 삼겹살 집에서 진행",
                                                    "roundNumber": 1,
                                                    "heldAt": "2025-07-05T09:00:00",
                                                    "category": "GATHERING",
                                                    "receipt": null,
                                                    "amount": 820000,
                                                    "gatheringMembers": [
                                                        {
                                                            "memberId": 1,
                                                                    "name": "정준원",
                                                                    "isJoined": true,
                                                                    "isCompleted": true
                                                        }, {
                                                            "memberId": 2,
                                                                    "name": "이한음",
                                                                    "isJoined": true,
                                                                    "isCompleted": false
                                                        }, {
                                                            "memberId": 3,
                                                                    "name": "신민철",
                                                                    "isJoined": false,
                                                                    "isCompleted": false
                                                        }
                                                    ]
                                                },
                                                {
                                                    "title": "2차 회식 - 맥주집",
                                                    "description": "근처 호프집에서 맥주먹음 ㅇㅇ 아 맥주 먹고 싶다...",
                                                    "roundNumber": 2,
                                                    "heldAt": "2025-07-05T09:00:00",
                                                    "category": "GATHERING",
                                                    "receipt": null,
                                                    "amount": 420000,
                                                    "gatheringMembers": [
                                                        {
                                                            "memberId": 1,
                                                                    "name": "정준원",
                                                                    "isJoined": true,
                                                                    "isCompleted": true
                                                        }, {
                                                            "memberId": 2,
                                                                    "name": "이한음",
                                                                    "isJoined": false,
                                                                    "isCompleted": false
                                                        }, {
                                                            "memberId": 3,
                                                                    "name": "신민철",
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
    fun findBill(billId: BillId): CustomResponse<Void>
}
