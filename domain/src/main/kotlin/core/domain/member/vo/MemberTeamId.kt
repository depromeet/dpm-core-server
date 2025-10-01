package core.domain.member.vo

@JvmInline
value class MemberTeamId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
