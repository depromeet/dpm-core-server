package core.domain.afterParty.vo

@JvmInline
value class AfterPartyInviteeId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
