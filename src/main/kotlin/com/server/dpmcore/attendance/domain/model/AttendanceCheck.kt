package com.server.dpmcore.attendance.domain.model

sealed class AttendanceCheck {
    data class Success(val status: AttendanceStatus) : AttendanceCheck()

    data object TooEarly : AttendanceCheck()
}
