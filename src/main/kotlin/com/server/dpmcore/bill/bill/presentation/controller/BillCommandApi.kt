package com.server.dpmcore.bill.bill.presentation.controller

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.presentation.dto.request.CreateBillRequest
import com.server.dpmcore.bill.bill.presentation.dto.request.SubmitBillParticipationConfirmRequest
import com.server.dpmcore.bill.bill.presentation.dto.response.BillPersistenceResponse
import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.member.member.domain.model.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Tag(name = "Bill", description = "정산 API")
interface BillCommandApi {
    @Operation(
        summary = "정산서 생성 또는 추가",
        description =
            "정산을 추가합니다. 각 차수의 회식과 정산서, 참여 멤버 등을 함께 추가해야 합니다. \n" +
                "추후에는 영수증 사진이 추가될 수 있습니다.",
        requestBody =
            RequestBody(
                description = "정산 생성 요청",
                required = true,
                content = [
                    Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CreateBillRequest::class),
                        examples = [
                            ExampleObject(
                                name = "정산 생성 요청 예시",
                                value = """
                                {
                                  "title": "17기 OT세션 공식 회식",
                                  "description": "OT 이후 첫 공식 회식 자리입니다. 운영진 및 디퍼 전체가 초대됩니다.",
                                  "billAccountId": 1,
                                  "invitedAuthorityIds": [
                                    1, 2
                                  ],
                                  "gatherings": [
                                    {
                                      "title": "1차 회식 - 고깃집",
                                      "description": "가까운 삼겹살 집에서 진행",
                                      "roundNumber": 1,
                                      "heldAt": "2025-07-22T05:55:34.606Z",
                                      "receipt": {
                                        "amount": 820000
                                      }
                                    }
                                  ]
                                }
                            """,
                            ),
                        ],
                    ),
                ],
            ),
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정산 추가 성공",
                content = [
                    Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "정산 추가 성공 응답",
                                value = """
                                    {
                                        "status": "CREATED",
                                        "message": "요청에 성공하여 리소스가 생성되었습니다.",
                                        "code": "GLOBAL-201-01",
                                        "data": {
                                            "billId": 11
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
    fun createBill(
        hostUserId: MemberId,
        @Valid createBillRequest: CreateBillRequest,
    ): CustomResponse<BillPersistenceResponse>

    @Operation(
        summary = "정산 참여 마감",
        description =
            "정산 참여를 마감합니다. 마감 이후에는 1/n 분할된 금액이 산출되기에 멤버를 추가할 수 없습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정산 참여 마감 성공",
                content = [
                    Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "정산 참여 마감 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공하였습니다.",
                                        "code": "GLOBAL-200-01",
                                        "data": {
                                            "billId": 11
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
    fun closeBillParticipation(billId: Long): CustomResponse<BillPersistenceResponse>

    @Operation(
        summary = "정산서 조회 처리",
        description = "정산서를 조회 처리합니다. 회식 참여 멤버의 조회 상태를 업데이트합니다.",
    )
    @ApiResponse(
        responseCode = "204",
        description = "정산서 조회 처리",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
            ),
        ],
    )
    fun markBillAsChecked(
        @Positive billId: BillId,
        memberId: MemberId,
    ): CustomResponse<Void>

    @Operation(
        summary = "정산서 생성 또는 추가",
        description =
            "정산을 추가합니다. 각 차수의 회식과 정산서, 참여 멤버 등을 함께 추가해야 합니다. \n" +
                "추후에는 영수증 사진이 추가될 수 있습니다.",
        requestBody =
            RequestBody(
                description = "정산 생성 요청",
                required = true,
                content = [
                    Content(
                        mediaType = APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CreateBillRequest::class),
                        examples = [
                            ExampleObject(
                                name = "정산 생성 요청 예시",
                                value = """
                                {
                                  "title": "17기 OT세션 공식 회식",
                                  "description": "OT 이후 첫 공식 회식 자리입니다. 운영진 및 디퍼 전체가 초대됩니다.",
                                  "billAccountId": 1,
                                  "invitedAuthorityIds": [
                                    1, 2
                                  ],
                                  "gatherings": [
                                    {
                                      "title": "1차 회식 - 고깃집",
                                      "description": "가까운 삼겹살 집에서 진행",
                                      "roundNumber": 1,
                                      "heldAt": "2025-07-22T05:55:34.606Z",
                                      "receipt": {
                                        "amount": 820000
                                      }
                                    }
                                  ]
                                }
                            """,
                            ),
                        ],
                    ),
                ],
            ),
    )
    @ApiResponse(
        responseCode = "204",
        description = "정산 참여 응답 처리",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
            ),
        ],
    )
    fun submitBillParticipationConfirm(
        billId: BillId,
        memberId: MemberId,
        submitBillParticipationConfirmRequest: SubmitBillParticipationConfirmRequest,
    ): CustomResponse<Void>
}
