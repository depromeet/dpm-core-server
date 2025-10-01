package core.domain.bill.vo

@JvmInline
value class BillAccountId(val value: Long) {
    override fun toString(): String = value.toString()
}
