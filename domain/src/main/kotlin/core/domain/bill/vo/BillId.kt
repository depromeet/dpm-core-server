package core.domain.bill.vo

@JvmInline
value class BillId(val value: Long) {
    override fun toString(): String = value.toString()
}
