package com.server.dpmcore.attendance.domain.model

@JvmInline
value class AttendanceId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
