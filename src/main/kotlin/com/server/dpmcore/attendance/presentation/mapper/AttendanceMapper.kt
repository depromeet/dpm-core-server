package com.server.dpmcore.attendance.presentation.mapper

import com.server.dpmcore.attendance.application.query.model.MemberAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.SessionAttendanceQueryModel
import com.server.dpmcore.attendance.domain.model.AttendanceStatus
import com.server.dpmcore.attendance.domain.port.inbound.command.AttendanceStatusUpdateCommand
import com.server.dpmcore.attendance.presentation.dto.request.AttendanceStatusUpdateRequest
import com.server.dpmcore.attendance.presentation.dto.response.AttendanceResponse
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendanceResponse
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendancesResponse
import com.server.dpmcore.attendance.presentation.dto.response.SessionAttendancesResponse
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.session.domain.model.SessionId
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
        members: List<MemberAttendanceQueryModel>,
        hasNext: Boolean,
        nextCursorId: Long?,
        impossibleThreshold: Int,
        atRiskThreshold: Int,
    ): MemberAttendancesResponse =
        MemberAttendancesResponse(
            members =
                members.map { member ->
                    MemberAttendanceResponse(
                        id = member.id,
                        name = member.name,
                        teamNumber = member.teamNumber,
                        part = member.part,
                        attendanceStatus = member.evaluateAttendanceStatus(impossibleThreshold, atRiskThreshold),
                    )
                },
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
}
