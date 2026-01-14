package core.domain.authorization.vo

@JvmInline
value class PermissionId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
