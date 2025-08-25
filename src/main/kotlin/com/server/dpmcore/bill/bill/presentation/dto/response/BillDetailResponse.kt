package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.model.BillStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class BillDetailResponse(
    @field:Schema(
        description = "정산 일련번호",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val billId: BillId,
    @field:Schema(
        description = "정산 제목",
        example = "OT 회식 정산",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val title: String,
    @field:Schema(
        description = "정산 설명",
        example = "OT 회식에 대한 정산 내역입니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    val description: String?,
    @field:Schema(
        description = "정산 주최자의 멤버 일련번호",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val hostUserId: Long,
    @field:Schema(
        description = "정산 총 금액",
        example = "2573400",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val billTotalAmount: Int,
    @field:Schema(
        description = "조회하는 멤버가 지불해야 할 분할된 정산 금액(참여한 회식의 분할 금액의 합)",
        example = "58600",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val billTotalSplitAmount: Int,
    @field:Schema(
        description = "정산 상태(OPEN, IN_PROGRESS, COMPLETED)",
        example = "IN_PROGRESS",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val billStatus: BillStatus,
    @field:Schema(
        description = "정산 생성 일시",
        example = "2025-08-03T12:30:00",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val createdAt: LocalDateTime,
    @field:Schema(
        description = "정산에 사용되는 계좌 일련번호(지금은 1번 무조건 보내면 됩니다.)",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val billAccountId: Long,
    @field:Schema(
        description = "정산에 초대된 멤버의 수(지금은 17기 디퍼와 운영진 총합입니다.)",
        example = "76",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val invitedMemberCount: Int,
    @field:Schema(
        description = "초대에 답변한 멤버의 수",
        example = "43",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val invitationSubmittedCount: Int,
    @field:Schema(
        description = "초대를 확인(열람)한 멤버 수",
        example = "49",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val invitationCheckedMemberCount: Int,
    @field:Schema(
        description = "초대를 확인(열람)한 여부",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val isViewed: Boolean,
    @field:Schema(
        description = "정산 참여 여부",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val isJoined: Boolean,
    @field:Schema(
        description = "정산 참여 제출 여부",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val isInvitationSubmitted: Boolean,
    val inviteAuthorities: List<BillDetailInviteAuthorityResponse> =
        BillDetailInviteAuthorityResponse.defaultInviteAuthorityResponse(),
    val gatherings: List<BillDetailGatheringResponse>,
)
