package com.server.dpmcore.bill.bill.domain.port.inbound

import com.server.dpmcore.bill.bill.domain.model.BillId

data class UpdateMemberListDepositCommand(
    val billId: BillId,
    val members: List<UpdateMemberListDepositMemberCommand>,
)
