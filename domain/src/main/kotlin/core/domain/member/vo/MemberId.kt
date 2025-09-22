package core.domain.member.vo

@JvmInline
value class MemberId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
