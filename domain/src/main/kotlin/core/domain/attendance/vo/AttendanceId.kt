package core.domain.attendance.vo

@JvmInline
value class AttendanceId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
