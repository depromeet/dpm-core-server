package core.application.bill.presentation.controller

import core.application.bill.presentation.request.CreateBillRequest
import core.application.bill.presentation.request.UpdateGatheringJoinsRequest
import core.application.bill.presentation.request.UpdateMemberDepositRequest
import core.application.bill.presentation.request.UpdateMemberListDepositRequest
import core.application.bill.presentation.response.BillPersistenceResponse
import core.application.common.exception.CustomResponse
import core.domain.bill.vo.BillId
import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
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
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정산 추가 성공",
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
        summary = "정산 참여 응답 제출 처리",
        description =
            "정산 초대에 대해 응답했음을 처리합니다.",
    )
    @ApiResponse(
        responseCode = "204",
        description = "정산 참여 응답 제출 응답 처리",
        content = [
            Content(
                mediaType = APPLICATION_JSON_VALUE,
                schema = Schema(implementation = CustomResponse::class),
            ),
        ],
    )
    fun submitBillParticipationConfirm(
        @Positive billId: BillId,
        memberId: MemberId,
    ): CustomResponse<Void>

    @ApiResponse(
        responseCode = "204",
        description = "정산(각 회식) 참여",
    )
    @Operation(
        summary = "정산의 각 회식에 대해 참여 저장",
        description = "정산 내의 각 회식에 대한 참여 여부를 표시합니다. '정산 참여 응답 제출 처리' API와 함께 호출됩니다.",
    )
    fun markAsGatheringJoined(
        @Positive billId: BillId,
        request: UpdateGatheringJoinsRequest,
        memberId: MemberId,
    ): CustomResponse<Void>

    @Operation(
        summary = "개별 멤버 정산 입금 상태 변경",
        description =
            "개별 멤버에 대해서 정산 입금 상태를 변경 합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "개별 멤버 정산 입금 상태 변경 성공",
            ),
        ],
    )
    fun updateMemberDeposit(
        @Positive billId: BillId,
        memberId: MemberId,
        @Valid updateMemberDepositRequest: UpdateMemberDepositRequest,
    ): CustomResponse<Void>

    @Operation(
        summary = "복수 멤버 정산 입금 상태 변경",
        description =
            "복수의 멤버에 대해서 정산 입금 상태를 변경합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "복수 멤버 정산 입금 상태 변경 성공",
            ),
        ],
    )
    fun updateMemberListDeposit(
        @Positive billId: BillId,
        @Valid updateMemberListDepositRequest: UpdateMemberListDepositRequest,
    ): CustomResponse<Void>
}
