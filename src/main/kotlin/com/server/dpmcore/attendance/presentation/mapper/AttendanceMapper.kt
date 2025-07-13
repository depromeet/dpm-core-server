package com.server.dpmcore.attendance.presentation.mapper

import com.server.dpmcore.attendance.application.query.model.SessionAttendanceQueryModel
import com.server.dpmcore.attendance.domain.model.AttendanceStatus
import com.server.dpmcore.attendance.presentation.dto.response.AttendanceResponse
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendanceResponse
import com.server.dpmcore.attendance.presentation.dto.response.SessionAttendanceResponse
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

    fun toSessionAttendanceResponse(
        members: List<SessionAttendanceQueryModel>,
        hasNext: Boolean,
        nextCursorId: Long?,
    ): SessionAttendanceResponse =
        SessionAttendanceResponse(
            members =
                members.map { member ->
                    MemberAttendanceResponse(
                        id = member.id,
                        name = member.name,
                        teamNumber = member.teamNumber,
                        part = member.part,
                        attendanceStatus = member.attendanceStatus,
                    )
                },
            hasNext = hasNext,
            nextCursorId = nextCursorId,
        )
}
