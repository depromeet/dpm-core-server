package com.server.dpmcore.bill.bill.domain.model

@JvmInline
value class BillId(val value: Long) {
    override fun toString(): String = value.toString()
}
