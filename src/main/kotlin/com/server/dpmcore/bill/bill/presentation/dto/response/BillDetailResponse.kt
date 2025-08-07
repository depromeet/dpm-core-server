package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.model.BillStatus
import java.time.LocalDateTime

data class BillDetailResponse(
    val billId: BillId,
    val title: String,
    val description: String?,
    val hostUserId: Long,
    val billTotalAmount: Int,
    val billTotalSplitAmount: Int,
    val billStatus: BillStatus,
    val createdAt: LocalDateTime,
    val billAccountId: Long,
    /** 초대된 총 멤버수 */
    val invitedMemberCount: Int,
    /** 초대에 답변을 제출한 멤버의 수 */
    val invitationSubmittedCount: Int,
    /** 초대를 확인한 멤버 수 */
    val invitationCheckedMemberCount: Int,
    val inviteAuthorities: List<BillDetailInviteAuthorityResponse> = BillDetailInviteAuthorityResponse.defaultInviteAuthorityResponse(),
    val gatherings: List<BillDetailGatheringResponse>,
)
