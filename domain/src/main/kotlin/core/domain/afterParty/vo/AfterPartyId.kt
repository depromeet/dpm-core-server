package core.domain.afterParty.vo

@JvmInline
value class AfterPartyId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
