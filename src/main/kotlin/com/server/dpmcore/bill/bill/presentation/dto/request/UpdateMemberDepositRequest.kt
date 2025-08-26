package com.server.dpmcore.bill.bill.presentation.dto.request

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.UpdateMemberDepositCommand
import com.server.dpmcore.member.member.domain.model.MemberId

data class UpdateMemberDepositRequest(
    val isDeposit: Boolean,
    val memo: String?,
) {
    fun toCommand(
        billId: BillId,
        memberId: MemberId,
    ) = UpdateMemberDepositCommand(
        billId = billId,
        memberId = memberId,
        isDeposit = isDeposit,
        memo = memo,
    )
}
