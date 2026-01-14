package core.domain.member.vo

@JvmInline
value class MemberCohortId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
