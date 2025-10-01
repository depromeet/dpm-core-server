package core.domain.authority.vo

@JvmInline
value class AuthorityId(val value: Long) {
    override fun toString(): String = value.toString()
}
