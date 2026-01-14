package core.domain.bill.port.inbound.command

import core.domain.bill.vo.BillId
import core.domain.member.vo.MemberId

data class UpdateMemberDepositCommand(
    val billId: BillId,
    val memberId: MemberId,
    val isDeposit: Boolean,
    val memo: String?,
)
