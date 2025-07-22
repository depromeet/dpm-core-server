package com.server.dpmcore.bill.billAccount.presentation.mapper

import com.server.dpmcore.bill.billAccount.domain.model.BillAccount
import com.server.dpmcore.bill.billAccount.domain.model.BillAccountId
import com.server.dpmcore.bill.billAccount.presentation.dto.response.BillAccountResponse
import com.server.dpmcore.bill.exception.BillException

object BillAccountMapper {
    fun toBillAccount(billAccountId: Long): BillAccount =
        BillAccount(
            id = BillAccountId(billAccountId),
        )

    fun toBillAccountResponse(billAccount: BillAccount): BillAccountResponse =
        BillAccountResponse(
            id = billAccount.id?.value ?: throw BillException.BillAccountIdRequiredException(),
            billAccountValue = billAccount.billAccountValue,
            accountHolderName = billAccount.accountHolderName,
            bankName = billAccount.bankName,
            accountType = billAccount.accountType.value,
        )
}
