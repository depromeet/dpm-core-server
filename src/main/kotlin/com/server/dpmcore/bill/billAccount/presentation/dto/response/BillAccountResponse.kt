package com.server.dpmcore.bill.billAccount.presentation.dto.response

data class BillAccountResponse(
    val id: Long,
    val billAccountValue: String,
    val accountHolderName: String,
    val bankName: String,
    val accountType: String,
)
