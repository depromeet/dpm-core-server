package core.domain.gathering.vo

@JvmInline
value class GatheringMemberId(val value: Long) {
    override fun toString(): String = value.toString()
}
