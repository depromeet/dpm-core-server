package core.domain.announcement.vo

@JvmInline
value class AssignmentId(val value: Long) {
    override fun toString(): String = value.toString()
}
