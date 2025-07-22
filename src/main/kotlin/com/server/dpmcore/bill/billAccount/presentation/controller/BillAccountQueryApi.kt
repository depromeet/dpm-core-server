package com.server.dpmcore.bill.billAccount.presentation.controller

import com.server.dpmcore.bill.billAccount.presentation.dto.response.BillAccountResponse
import com.server.dpmcore.common.exception.CustomResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "정산 계좌(Bill Account)")
interface BillAccountQueryApi {
    @Operation(
        summary = "정산 계좌 조회",
        description =
            "정산 계좌를 조회합니다. 계좌 정보 혹은 송금 URL(카카오페이 등)이 올 수 있습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정산 계좌 정보 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = CustomResponse::class),
                        examples = [
                            ExampleObject(
                                name = "정산 계좌 정보 조회 성공 응답",
                                value = """
                                    {
                                        "status": "OK",
                                        "message": "요청에 성공하였습니다.",
                                        "code": "BILL_ACCOUNT-001",
                                        "data": {
                                            "id": 1,
                                            "billAccountValue": "12345-00-123456",
                                            "accountHolderName": "정준원",
                                            "bankName": "KB 국민",
                                            "accountType": "ACCOUNT",
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
    fun getBillAccount(billAccountId: Long): CustomResponse<BillAccountResponse>
}
