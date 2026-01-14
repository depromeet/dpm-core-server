package core.domain.member.vo

@JvmInline
value class MemberRoleId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
