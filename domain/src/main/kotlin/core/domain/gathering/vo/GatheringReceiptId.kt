package core.domain.gathering.vo

@JvmInline
value class GatheringReceiptId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
