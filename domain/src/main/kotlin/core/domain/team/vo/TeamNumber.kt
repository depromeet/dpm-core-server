package core.domain.team.vo

@JvmInline
value class TeamNumber(
    val value: Int,
) {
    companion object {
        fun defaultValue(): TeamNumber = TeamNumber(0)
    }
    constructor(v: Int?) : this(v ?: 0)

    override fun toString(): String = value.toString()
}
