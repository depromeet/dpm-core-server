package com.server.dpmcore.attendance.presentation.mapper

import com.server.dpmcore.attendance.domain.model.AttendanceStatus
import com.server.dpmcore.attendance.presentation.dto.response.AttendanceResponse
import com.server.dpmcore.session.presentation.mapper.TimeMapper.instantToLocalDateTime
import java.time.Instant

object AttendanceMapper {
    fun toAttendanceResponse(
        attendanceStatus: AttendanceStatus,
        attendedAt: Instant,
    ): AttendanceResponse =
        AttendanceResponse(
            attendanceStatus = attendanceStatus.name,
            attendedAt = instantToLocalDateTime(attendedAt),
        )
}
