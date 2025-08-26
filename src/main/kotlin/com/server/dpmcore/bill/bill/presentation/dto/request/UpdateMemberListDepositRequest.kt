package com.server.dpmcore.bill.bill.presentation.dto.request

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.UpdateMemberListDepositCommand
import com.server.dpmcore.bill.bill.domain.port.inbound.UpdateMemberListDepositMemberCommand
import com.server.dpmcore.member.member.domain.model.MemberId
import jakarta.validation.constraints.NotEmpty

data class UpdateMemberListDepositRequest(
    @NotEmpty
    val members: List<UpdateMemberDepositRequest>,
) {
    data class UpdateMemberDepositRequest(
        val memberId: MemberId,
        val isDeposit: Boolean,
    )

    fun toCommand(billId: BillId) =
        UpdateMemberListDepositCommand(
            billId = billId,
            members =
                members.map {
                    UpdateMemberListDepositMemberCommand(
                        memberId = it.memberId,
                        isDeposit = it.isDeposit,
                    )
                },
        )
}
