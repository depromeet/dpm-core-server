package core.domain.absencereason.vo

@JvmInline
value class AbsenceReasonId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
