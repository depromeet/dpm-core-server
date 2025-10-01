package core.domain.team.vo

@JvmInline
value class TeamId(val value: Long) {
    override fun toString(): String = value.toString()
}
