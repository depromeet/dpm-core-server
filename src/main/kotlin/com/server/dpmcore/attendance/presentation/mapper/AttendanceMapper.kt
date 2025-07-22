package com.server.dpmcore.attendance.presentation.mapper

import com.server.dpmcore.attendance.application.query.model.MemberDetailAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.MemberSessionAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.MyDetailAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.SessionAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.SessionDetailAttendanceQueryModel
import com.server.dpmcore.attendance.domain.model.AttendanceStatus
import com.server.dpmcore.attendance.domain.port.inbound.command.AttendanceStatusUpdateCommand
import com.server.dpmcore.attendance.presentation.dto.request.AttendanceStatusUpdateRequest
import com.server.dpmcore.attendance.presentation.dto.response.AttendanceResponse
import com.server.dpmcore.attendance.presentation.dto.response.DetailAttendancesBySessionResponse
import com.server.dpmcore.attendance.presentation.dto.response.DetailMemberAttendancesResponse
import com.server.dpmcore.attendance.presentation.dto.response.DetailMemberInfo
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendanceResponse
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendancesResponse
import com.server.dpmcore.attendance.presentation.dto.response.MemberDetailAttendanceCountInfo
import com.server.dpmcore.attendance.presentation.dto.response.MemberDetailSessionInfo
import com.server.dpmcore.attendance.presentation.dto.response.MyDetailAttendanceBySessionResponse
import com.server.dpmcore.attendance.presentation.dto.response.MyDetailAttendanceInfo
import com.server.dpmcore.attendance.presentation.dto.response.MyDetailAttendanceSessionInfo
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
                    date = instantToLocalDateTime(model.sessionDate),
                ),
            attendance =
                DetailAttendancesBySessionResponse.DetailAttendance(
                    status = model.attendanceStatus,
                    attendedAt = instantToLocalDateTime(model.attendedAt),
                ),
        )

    fun toDetailMemberAttendancesResponse(
        memberAttendanceModel: MemberDetailAttendanceQueryModel,
        sessionAttendancesModel: List<MemberSessionAttendanceQueryModel>,
        evaluation: String,
    ): DetailMemberAttendancesResponse =
        DetailMemberAttendancesResponse(
            member =
                DetailMemberInfo(
                    id = memberAttendanceModel.memberId,
                    name = memberAttendanceModel.memberName,
                    teamNumber = memberAttendanceModel.teamNumber,
                    part = memberAttendanceModel.part,
                    attendanceStatus = evaluation,
                ),
            attendance =
                MemberDetailAttendanceCountInfo(
                    presentCount = memberAttendanceModel.presentCount,
                    lateCount = memberAttendanceModel.lateCount,
                    excusedAbsentCount = memberAttendanceModel.excusedAbsentCount,
                    absentCount = memberAttendanceModel.onlineAbsentCount + memberAttendanceModel.offlineAbsentCount,
                ),
            sessions =
                sessionAttendancesModel.map { session ->
                    MemberDetailSessionInfo(
                        id = session.sessionId,
                        week = session.sessionWeek,
                        eventName = session.sessionEventName,
                        date = instantToLocalDateTime(session.sessionDate),
                        attendanceStatus = session.sessionAttendanceStatus,
                    )
                },
        )

    fun toMyDetailAttendanceBySessionResponse(myAttendanceModel: MyDetailAttendanceQueryModel) =
        MyDetailAttendanceBySessionResponse(
            attendance =
                MyDetailAttendanceInfo(
                    status = myAttendanceModel.attendanceStatus,
                    attendedAt = instantToLocalDateTime(myAttendanceModel.attendedAt),
                ),
            session =
                MyDetailAttendanceSessionInfo(
                    week = myAttendanceModel.sessionWeek,
                    eventName = myAttendanceModel.sessionEventName,
                    date = instantToLocalDateTime(myAttendanceModel.sessionDate),
                    place = myAttendanceModel.sessionPlace,
                ),
        )
}
