package core.domain.member.vo

@JvmInline
value class MemberPermissionId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
