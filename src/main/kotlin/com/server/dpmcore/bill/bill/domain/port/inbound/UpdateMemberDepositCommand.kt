package com.server.dpmcore.bill.bill.domain.port.inbound

import com.server.dpmcore.member.member.domain.model.MemberId

data class UpdateMemberDepositCommand(
    val memberId: MemberId,
    val isDeposit: Boolean,
)
