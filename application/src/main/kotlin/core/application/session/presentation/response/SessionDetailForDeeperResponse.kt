package core.application.session.presentation.response

import core.domain.attendance.enums.AttendanceStatus
import core.domain.session.vo.SessionId
import java.time.LocalDateTime

data class SessionDetailForDeeperResponse(
    val id: SessionId,
    val week: Int,
    val name: String,
    val place: String,
    val isOnline: Boolean,
    val date: LocalDateTime,
    val attendanceStart: LocalDateTime,
    val lateStart: LocalDateTime,
    val absentStart: LocalDateTime,
    val attendanceStatus: AttendanceStatus,
    val attendedAt: LocalDateTime?,
) {
    companion object {
        fun of(
            id: SessionId,
            week: Int,
            name: String,
            place: String,
            isOnline: Boolean,
            date: LocalDateTime,
            attendanceStart: LocalDateTime,
            lateStart: LocalDateTime,
            absentStart: LocalDateTime,
            attendanceStatus: AttendanceStatus,
            attendedAt: LocalDateTime?,
        ): SessionDetailForDeeperResponse =
            SessionDetailForDeeperResponse(
                id = id,
                week = week,
                name = name,
                place = place,
                isOnline = isOnline,
                date = date,
                attendanceStart = attendanceStart,
                lateStart = lateStart,
                absentStart = absentStart,
                attendanceStatus = attendanceStatus,
                attendedAt = attendedAt,
            )
    }
}
