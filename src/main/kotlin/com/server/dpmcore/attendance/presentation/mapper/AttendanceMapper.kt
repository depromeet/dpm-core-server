package com.server.dpmcore.attendance.presentation.mapper

import com.server.dpmcore.attendance.application.query.model.DetailMemberAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.MemberSessionAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.SessionAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.SessionDetailAttendanceQueryModel
import com.server.dpmcore.attendance.domain.model.AttendanceStatus
import com.server.dpmcore.attendance.domain.port.inbound.command.AttendanceStatusUpdateCommand
import com.server.dpmcore.attendance.presentation.dto.request.AttendanceStatusUpdateRequest
import com.server.dpmcore.attendance.presentation.dto.response.AttendanceResponse
import com.server.dpmcore.attendance.presentation.dto.response.DetailAttendance
import com.server.dpmcore.attendance.presentation.dto.response.DetailAttendancesBySessionResponse
import com.server.dpmcore.attendance.presentation.dto.response.DetailMember
import com.server.dpmcore.attendance.presentation.dto.response.DetailMemberAttendancesResponse
import com.server.dpmcore.attendance.presentation.dto.response.DetailSession
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendanceResponse
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendancesResponse
import com.server.dpmcore.attendance.presentation.dto.response.SessionAttendancesResponse
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.session.domain.model.SessionId
import com.server.dpmcore.session.presentation.mapper.TimeMapper.instantToLocalDateTimeString
import java.time.Instant
import java.time.format.DateTimeFormatter

object AttendanceMapper {
    fun toAttendanceResponse(
        attendanceStatus: AttendanceStatus,
        attendedAt: Instant,
    ): AttendanceResponse =
        AttendanceResponse(
            attendanceStatus = attendanceStatus.name,
            attendedAt = instantToLocalDateTimeString(attendedAt),
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
        evaluation: String,
    ): DetailAttendancesBySessionResponse =
        DetailAttendancesBySessionResponse(
            member =
                DetailAttendancesBySessionResponse.DetailMember(
                    id = model.memberId,
                    name = model.memberName,
                    teamNumber = model.teamNumber,
                    part = model.part,
                    attendanceStatus = evaluation,
                ),
            session =
                DetailAttendancesBySessionResponse.DetailSession(
                    id = model.sessionId,
                    week = model.sessionWeek,
                    eventName = model.sessionEventName,
                    date = model.sessionDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                ),
            attendance =
                DetailAttendancesBySessionResponse.DetailAttendance(
                    status = model.attendanceStatus,
                    attendedAt = model.attendedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                ),
        )

    fun toDetailMemberAttendancesResponse(
        memberAttendanceModel: DetailMemberAttendanceQueryModel,
        sessionAttendancesModel: List<MemberSessionAttendanceQueryModel>,
        evaluation: String,
    ): DetailMemberAttendancesResponse =
        DetailMemberAttendancesResponse(
            member =
                DetailMember(
                    id = memberAttendanceModel.memberId,
                    name = memberAttendanceModel.memberName,
                    teamNumber = memberAttendanceModel.teamNumber,
                    part = memberAttendanceModel.part,
                    attendanceStatus = evaluation,
                ),
            attendance =
                DetailAttendance(
                    presentCount = memberAttendanceModel.presentCount,
                    lateCount = memberAttendanceModel.lateCount,
                    excusedAbsentCount = memberAttendanceModel.excusedAbsentCount,
                    absentCount = memberAttendanceModel.onlineAbsentCount + memberAttendanceModel.offlineAbsentCount,
                ),
            sessions =
                sessionAttendancesModel.map { session ->
                    DetailSession(
                        id = session.sessionId,
                        week = session.sessionWeek,
                        eventName = session.sessionEventName,
                        date = session.sessionDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        attendanceStatus = session.sessionAttendanceStatus,
                    )
                },
        )
}
