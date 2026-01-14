package core.domain.authorization.vo

@JvmInline
value class RoleId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
