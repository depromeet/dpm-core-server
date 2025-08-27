package com.server.dpmcore.bill.bill.presentation.dto.request

import com.server.dpmcore.bill.bill.domain.model.BillId
import com.server.dpmcore.bill.bill.domain.port.inbound.UpdateMemberDepositCommand
import com.server.dpmcore.member.member.domain.model.MemberId
import io.swagger.v3.oas.annotations.media.Schema

data class UpdateMemberDepositRequest(
    @field:Schema(
        description = "입금 완료 여부",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val isDeposit: Boolean,
    @field:Schema(
        description = "상태 변경 메모",
        example = "1차는 참여하고, 2차는 불참했습니다.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
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
