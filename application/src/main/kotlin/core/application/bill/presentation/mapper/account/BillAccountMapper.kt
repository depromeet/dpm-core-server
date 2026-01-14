package core.application.bill.presentation.mapper.account

import core.application.bill.application.exception.account.BillAccountIdRequiredException
import core.application.bill.presentation.response.account.BillAccountResponse
import core.domain.bill.aggregate.BillAccount
import core.domain.bill.vo.BillAccountId

object BillAccountMapper {
    fun toBillAccount(billAccountId: Long): BillAccount =
        BillAccount(
            id = BillAccountId(billAccountId),
        )

    fun toBillAccountResponse(billAccount: BillAccount): BillAccountResponse =
        BillAccountResponse(
            id = billAccount.id?.value ?: throw BillAccountIdRequiredException(),
            billAccountValue = billAccount.billAccountValue,
            accountHolderName = billAccount.accountHolderName,
            bankName = billAccount.bankName,
            accountType = billAccount.accountType.value,
        )
}
