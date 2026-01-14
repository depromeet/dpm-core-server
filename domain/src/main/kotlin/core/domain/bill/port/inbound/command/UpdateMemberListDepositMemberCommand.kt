package core.domain.bill.port.inbound.command

import core.domain.member.vo.MemberId

data class UpdateMemberListDepositMemberCommand(
    val memberId: MemberId,
    val isDeposit: Boolean,
)
