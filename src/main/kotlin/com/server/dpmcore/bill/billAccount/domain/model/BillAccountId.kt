package com.server.dpmcore.bill.billAccount.domain.model

@JvmInline
value class BillAccountId(val value: Long) {
    override fun toString(): String = value.toString()
}
