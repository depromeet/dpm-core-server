package core.domain.image.vo

@JvmInline
value class ImageId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
