package com.server.dpmcore.bill.billAccount.persentation.mapper

import com.server.dpmcore.bill.billAccount.domain.model.BillAccount
import com.server.dpmcore.bill.billAccount.domain.model.BillAccountId

object BillAccountMapper {
    fun toBillAccount(billAccountId: Long): BillAccount =
        BillAccount(
            id = BillAccountId(billAccountId),
        )
}
