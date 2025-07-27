package com.server.dpmcore.bill.bill.presentation.dto.response

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.model.BillStatus
import java.time.LocalDateTime

data class BillListDetailResponse(
    val billId: BillId,
    val title: String,
    val description: String? = null,
    val billTotalAmount: Int,
    val billStatus: BillStatus,
    val createdAt: LocalDateTime,
    val billAccountId: Long,
    /** 초대된 총 멤버수 */
    val invitedMemberCount: Int,
    /** 초대에 답변한 멤버의 수 */
    val invitationConfirmedCount: Int,
    /** 초대를 확인한 멤버 수 */
    val invitationCheckedMemberCount: Int,
    val inviteAuthorities: List<BillListInviteAuthorityDetailResponse>? = BillListInviteAuthorityDetailResponse.defaultInviteAuthorityResponse(),
    val gatherings: List<BillListGatheringDetailResponse>,
)
