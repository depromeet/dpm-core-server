package com.server.dpmcore.attendance.presentation.mapper

import com.server.dpmcore.attendance.application.query.model.SessionAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.SessionDetailAttendanceQueryModel
import com.server.dpmcore.attendance.domain.model.AttendanceStatus
import com.server.dpmcore.attendance.domain.port.inbound.command.AttendanceStatusUpdateCommand
import com.server.dpmcore.attendance.presentation.dto.request.AttendanceStatusUpdateRequest
import com.server.dpmcore.attendance.presentation.dto.response.AttendanceResponse
import com.server.dpmcore.attendance.presentation.dto.response.DetailAttendanceBySessionResponse
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendanceResponse
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendancesResponse
import com.server.dpmcore.attendance.presentation.dto.response.SessionAttendancesResponse
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.session.domain.model.SessionId
import com.server.dpmcore.session.presentation.mapper.TimeMapper.instantToLocalDateTime
import java.time.Instant
import java.time.format.DateTimeFormatter

object AttendanceMapper {
    fun toAttendanceResponse(
        attendanceStatus: AttendanceStatus,
        attendedAt: Instant,
    ): AttendanceResponse =
        AttendanceResponse(
            attendanceStatus = attendanceStatus.name,
            attendedAt = instantToLocalDateTime(attendedAt),
        )

    fun toSessionAttendancesResponse(
        members: List<SessionAttendanceQueryModel>,
        hasNext: Boolean,
        nextCursorId: Long?,
    ): SessionAttendancesResponse =
        SessionAttendancesResponse(
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

    fun toMemberAttendancesResponse(
        members: List<MemberAttendanceResponse>,
        hasNext: Boolean,
        nextCursorId: Long?,
    ): MemberAttendancesResponse =
        MemberAttendancesResponse(
            members = members,
            hasNext = hasNext,
            nextCursorId = nextCursorId,
        )

    fun toAttendanceStatusUpdateCommand(
        sessionId: SessionId,
        memberId: MemberId,
        request: AttendanceStatusUpdateRequest,
    ): AttendanceStatusUpdateCommand =
        AttendanceStatusUpdateCommand(
            sessionId = sessionId,
            memberId = memberId,
            attendanceStatus = AttendanceStatus.valueOf(request.attendanceStatus),
        )

    fun toDetailAttendanceBySessionResponse(
        model: SessionDetailAttendanceQueryModel,
        attendanceStatus: String,
    ): DetailAttendanceBySessionResponse =
        DetailAttendanceBySessionResponse(
            member =
                DetailAttendanceBySessionResponse.DetailMember(
                    id = model.memberId,
                    name = model.memberName,
                    teamNumber = model.teamNumber,
                    part = model.part,
                    attendanceStatus = attendanceStatus,
                ),
            session =
                DetailAttendanceBySessionResponse.DetailSession(
                    id = model.sessionId,
                    week = model.sessionWeek,
                    eventName = model.sessionEventName,
                    date = model.sessionDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                ),
            attendance =
                DetailAttendanceBySessionResponse.DetailAttendance(
                    status = model.attendanceStatus,
                    attendedAt = model.attendedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                ),
        )
}
