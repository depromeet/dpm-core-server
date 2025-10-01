package core.domain.bill.port.inbound.command

import core.domain.bill.vo.BillId

data class UpdateMemberListDepositCommand(
    val billId: BillId,
    val members: List<UpdateMemberListDepositMemberCommand>,
)
