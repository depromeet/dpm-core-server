package com.server.dpmcore.bill.bill.presentation.dto.request

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.UpdateMemberListDepositCommand
import com.server.dpmcore.bill.bill.domain.port.inbound.UpdateMemberListDepositMemberCommand
import com.server.dpmcore.member.member.domain.model.MemberId
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

data class UpdateMemberListDepositRequest(
    @NotEmpty
    val members: List<UpdateMemberDepositRequest>,
) {
    data class UpdateMemberDepositRequest(
        @field:Schema(
            description = "멤버 식별자",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
        val memberId: MemberId,
        @field:Schema(
            description = "입금 완료 여부",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED,
        )
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
