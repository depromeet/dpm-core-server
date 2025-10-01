package core.application.bill.presentation.response.account

data class BillAccountResponse(
    val id: Long,
    val billAccountValue: String,
    val accountHolderName: String,
    val bankName: String,
    val accountType: String,
)
