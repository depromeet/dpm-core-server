package core.application.attendance.presentation.mapper

import core.application.attendance.presentation.request.AttendanceStatusUpdateRequest
import core.application.attendance.presentation.response.AttendanceResponse
import core.application.attendance.presentation.response.DetailAttendancesBySessionResponse
import core.application.attendance.presentation.response.DetailMemberAttendancesResponse
import core.application.attendance.presentation.response.DetailMemberInfo
import core.application.attendance.presentation.response.MemberAttendanceResponse
import core.application.attendance.presentation.response.MemberAttendancesResponse
import core.application.attendance.presentation.response.MemberDetailAbsenceReasonImageInfo
import core.application.attendance.presentation.response.MemberDetailAbsenceReasonInfo
import core.application.attendance.presentation.response.MemberDetailAttendanceCountInfo
import core.application.attendance.presentation.response.MemberDetailSessionInfo
import core.application.attendance.presentation.response.MyDetailAttendanceBySessionResponse
import core.application.attendance.presentation.response.MyDetailAttendanceInfo
import core.application.attendance.presentation.response.MyDetailAttendanceSessionInfo
import core.application.attendance.presentation.response.SessionRosterMemberResponse
import core.application.attendance.presentation.response.SessionRosterResponse
import core.application.common.converter.TimeMapper.instantToLocalDateTime
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceStatusUpdateCommand
import core.domain.attendance.port.outbound.query.MemberAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MemberSessionAttendanceQueryModel
import core.domain.attendance.port.outbound.query.MyDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionDetailAttendanceQueryModel
import core.domain.attendance.port.outbound.query.SessionRosterQueryModel
import core.domain.member.vo.MemberId
import core.domain.session.vo.SessionId
import java.time.Instant

object AttendanceMapper {
    private const val ONLINE_PLACE = "온라인"

    fun toAttendanceResponse(
        attendanceStatus: AttendanceStatus,
        attendedAt: Instant,
    ): AttendanceResponse =
        AttendanceResponse(
            attendanceStatus = attendanceStatus.name,
            attendedAt = instantToLocalDateTime(attendedAt),
        )

    /** 운영진이 바꾼 기록(updatedAt 있음)은 attendedAt 을 null 로 준다. 운영진 변경이 인증 시각을 지우기 전의 기록도 같다. */
    fun toSessionRosterResponse(
        members: List<SessionRosterQueryModel>,
        myTeamNumber: Int?,
    ): SessionRosterResponse =
        SessionRosterResponse(
            members =
                members.map { member ->
                    val isManuallyUpdated = member.updatedAt != null
                    SessionRosterMemberResponse(
                        id = member.memberId,
                        name = member.name,
                        teamNumber = member.teamNumber,
                        isAdmin = member.isAdmin,
                        part = member.part,
                        attendanceStatus = member.attendanceStatus,
                        attendedAt = if (isManuallyUpdated) null else instantToLocalDateTime(member.attendedAt),
                        isManuallyUpdated = isManuallyUpdated,
                        absenceReason = member.absenceReason,
                    )
                },
            myTeamNumber = myTeamNumber,
        )

    fun toMemberAttendanceResponse(
        member: MemberAttendanceQueryModel,
        evaluation: String,
    ): MemberAttendanceResponse =
        MemberAttendanceResponse(
            id = member.id,
            name = member.name,
            teamNumber = member.teamNumber,
            isAdmin = member.isAdmin,
            part = member.part,
            attendanceStatus = evaluation,
        )

    fun toMemberAttendancesResponse(
        members: List<MemberAttendanceResponse>,
        myTeamNumber: Int?,
        totalElements: Int,
    ): MemberAttendancesResponse =
        MemberAttendancesResponse(
            members = members,
            myTeamNumber = myTeamNumber,
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
            attendanceStatus = request.attendanceStatus,
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
                    isAdmin = model.isAdmin,
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
                    isAdmin = memberAttendanceModel.isAdmin,
                    part = memberAttendanceModel.part,
                    attendanceStatus = evaluation,
                ),
            attendance =
                MemberDetailAttendanceCountInfo(
                    presentCount = memberAttendanceModel.summary.presentCount,
                    lateCount = memberAttendanceModel.summary.lateCount,
                    excusedAbsentCount = memberAttendanceModel.summary.excusedAbsentCount,
                    absentCount = memberAttendanceModel.summary.absentCount,
                ),
            sessions =
                sessionAttendancesModel.map { session ->
                    MemberDetailSessionInfo(
                        id = session.sessionId,
                        week = session.sessionWeek,
                        eventName = session.sessionEventName,
                        date = instantToLocalDateTime(session.sessionDate),
                        attendanceStatus = session.sessionAttendanceStatus,
                        attendedAt = session.attendedAt?.let { instantToLocalDateTime(it) },
                        isOnline = session.sessionIsOnline,
                        place = if (session.sessionIsOnline) ONLINE_PLACE else session.sessionPlace,
                        absenceReason =
                            session.absenceReason?.let {
                                MemberDetailAbsenceReasonInfo(
                                    id = it.id,
                                    contents = it.contents,
                                    status = it.status,
                                    imageIds = it.images.map { image -> image.imageId },
                                    images =
                                        it.images.map { image ->
                                            MemberDetailAbsenceReasonImageInfo(image.imageId, image.fileName)
                                        },
                                )
                            },
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
