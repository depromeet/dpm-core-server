package core.domain.deviceToken.vo

@JvmInline
value class DeviceTokenId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
