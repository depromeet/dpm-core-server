package core.domain.gathering.vo

@JvmInline
value class GatheringReceiptPhotoId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
