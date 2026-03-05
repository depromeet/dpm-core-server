package core.domain.cohort.vo

@JvmInline
value class AuthorityId(val value: Long) {
    override fun toString(): String = value.toString()
}
