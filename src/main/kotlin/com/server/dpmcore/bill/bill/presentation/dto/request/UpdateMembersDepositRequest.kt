package com.server.dpmcore.bill.bill.presentation.dto.request

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.UpdateMemberDepositCommand
import com.server.dpmcore.bill.bill.domain.port.inbound.UpdateMembersDepositCommand
import com.server.dpmcore.member.member.domain.model.MemberId
import jakarta.validation.constraints.NotEmpty

data class UpdateMembersDepositRequest(
    @NotEmpty
    val members: List<UpdateMemberDepositRequest>,
) {
    data class UpdateMemberDepositRequest(
        val memberId: MemberId,
        val isDeposit: Boolean,
    )

    fun toCommand(billId: BillId) =
        UpdateMembersDepositCommand(
            billId = billId,
            members =
                members.map {
                    UpdateMemberDepositCommand(
                        memberId = it.memberId,
                        isDeposit = it.isDeposit,
                    )
                },
        )
}
