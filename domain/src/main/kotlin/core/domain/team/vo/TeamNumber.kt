package core.domain.team.vo

@JvmInline
value class TeamNumber(val value: Int) {
    override fun toString(): String = value.toString()
}
