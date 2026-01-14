package core.domain.cohort.vo

@JvmInline
value class CohortId(val value: Long) {
    override fun toString(): String = value.toString()
}
