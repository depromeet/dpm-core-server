package core.application.attendance.presentation.mapper

import core.application.attendance.presentation.request.AttendanceStatusUpdateRequest
import core.application.attendance.presentation.response.AttendanceResponse
import core.application.attendance.presentation.response.DetailAttendancesBySessionResponse
import core.application.attendance.presentation.response.DetailMemberAttendancesResponse
import core.application.attendance.presentation.response.DetailMemberInfo
import core.application.attendance.presentation.response.MemberAttendanceResponse
import core.application.attendance.presentation.response.MemberAttendancesResponse
import core.application.attendance.presentation.response.MemberDetailAttendanceCountInfo
import core.application.attendance.presentation.response.MemberDetailSessionInfo
import core.application.attendance.presentation.response.MyDetailAttendanceBySessionResponse
import core.application.attendance.presentation.response.MyDetailAttendanceInfo
import core.application.attendance.presentation.response.MyDetailAttendanceSessionInfo
import core.application.attendance.presentation.response.MyTeamFilterResponse
import core.application.attendance.presentation.response.SessionAttendancesResponse
import core.application.session.presentation.mapper.TimeMapper.instantToLocalDateTime
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceStatusUpdateCommand
import core.domain.attendance.port.outbound.query.MemberDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberSessionAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MyDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionDetailAttendanceQueryModel
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
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
        onlyMyTeam: Boolean,
        myTeamNumber: Int?,
        hasNext: Boolean,
        nextCursorId: Long?,
        totalElements: Int,
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
            filter = MyTeamFilterResponse(myTeamNumber, onlyMyTeam),
            hasNext = hasNext,
            nextCursorId = nextCursorId,
            totalElements = totalElements,
        )

    fun toMemberAttendancesResponse(
        members: List<MemberAttendanceResponse>,
        onlyMyTeam: Boolean,
        myTeamNumber: Int?,
        hasNext: Boolean,
        nextCursorId: Long?,
        totalElements: Int,
    ): MemberAttendancesResponse =
        MemberAttendancesResponse(
            members = members,
            filter = MyTeamFilterResponse(myTeamNumber, onlyMyTeam),
            hasNext = hasNext,
            nextCursorId = nextCursorId,
            totalElements = totalElements,
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
                    attendedAt = model.attendedAt?.let { instantToLocalDateTime(it) },
                    updatedAt = model.updatedAt?.let { instantToLocalDateTime(it) },
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
                    earlyLeaveCount = memberAttendanceModel.earlyLeaveCount,
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
                    attendedAt = myAttendanceModel.attendedAt?.let { instantToLocalDateTime(it) },
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
