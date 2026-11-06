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
    val lateParticipationDeadline: LocalDateTime,
    /** 조회 시점에 이 회원이 코드로 늦은 참여를 등록할 수 있는지 여부. */
    val canRegisterLateParticipation: Boolean,
    /** 운영진(ORGANIZER/CORE/MASTER)은 false. 디퍼 출석 배너·인증 CTA 노출에 쓴다. */
    val canRecordAttendance: Boolean = true,
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
            lateParticipationDeadline: LocalDateTime,
            canRegisterLateParticipation: Boolean,
            canRecordAttendance: Boolean = true,
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
                lateParticipationDeadline = lateParticipationDeadline,
                canRegisterLateParticipation = canRegisterLateParticipation,
                canRecordAttendance = canRecordAttendance,
            )
    }
}
