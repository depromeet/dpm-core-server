package com.server.dpmcore.bill.bill.presentation.controller

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.presentation.dto.response.BillDetailResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillListResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillMemberSubmittedListResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.BillSummaryListByMemberResponse
import com.server.dpmcore.bill.bill.presentation.dto.response.SubmittedParticipantEachGatheringResponse
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
        memberId: MemberId,
    ): CustomResponse<BillDetailResponse>

    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정산서 목록 조회 성공",
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
    )
    @Operation(
        summary = "정산서 멤버별 최종 금액 조회 API",
        description = "정산서에 참여한 멤버들의 최종 금액을 목록 조회합니다.",
    )
    fun getMemberBillSummaries(
        @Positive billId: BillId,
    ): CustomResponse<BillSummaryListByMemberResponse>

    @ApiResponse(
        responseCode = "200",
        description = "이전에 제출한 정산 참여 여부 조회 성공",
    )
    @Operation(
        summary = "이전에 제출한 정산 참여 여부 조회 API",
        description = "해당 멤버가 이전에 제출했던 회식 별 참석 여부를 조회합니다.",
    )
    fun getSubmittedParticipantEachGathering(
        @Positive billId: BillId,
        memberId: MemberId,
    ): CustomResponse<SubmittedParticipantEachGatheringResponse>

    @ApiResponse(
        responseCode = "200",
        description = "정산 참여 제출 여부 조회 성공",
    )
    @Operation(
        summary = "정산 참여 제출 여부 조회 API",
        description = "초대된 정산에 참여 여부를 제출한 멤버들의 목록을 조회합니다.",
    )
    fun getBillMemberSubmittedList(
        @Positive billId: BillId,
        memberId: MemberId,
    ): CustomResponse<BillMemberSubmittedListResponse>
}
