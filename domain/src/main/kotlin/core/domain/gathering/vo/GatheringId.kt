package core.domain.gathering.vo

@JvmInline
value class GatheringId(val value: Long) {
    override fun toString(): String = value.toString()
}
