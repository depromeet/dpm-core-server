package core.application.attendance.application.service

import core.application.attendance.application.exception.AttendanceNotFoundException
import core.application.attendance.presentation.mapper.AttendanceMapper
import core.application.attendance.presentation.response.DetailAttendancesBySessionResponse
import core.application.attendance.presentation.response.DetailMemberAttendancesResponse
import core.application.attendance.presentation.response.MemberAttendanceResponse
import core.application.attendance.presentation.response.MemberAttendancesResponse
import core.application.attendance.presentation.response.MyDetailAttendanceBySessionResponse
import core.application.attendance.presentation.response.SessionAttendancesResponse
import core.application.common.extension.paginate
import core.application.member.application.service.MemberQueryService
import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.port.inbound.query.GetAttendancesBySessionWeekQuery
import core.domain.attendance.port.inbound.query.GetDetailAttendanceBySessionQuery
import core.domain.attendance.port.inbound.query.GetDetailMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMemberAttendancesQuery
import core.domain.attendance.port.inbound.query.GetMyAttendanceBySessionQuery
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.session.vo.SessionId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AttendanceQueryService(
    private val memberQueryService: MemberQueryService,
    private val attendancePersistencePort: AttendancePersistencePort,
    private val attendanceGraduationEvaluator: AttendanceGraduationEvaluator,
) {
    fun getAttendancesBySession(query: GetAttendancesBySessionWeekQuery): SessionAttendancesResponse {
        val myTeamNumber =
            query.onlyMyTeam
                ?.let { memberQueryService.getMemberTeamNumber(query.memberId) }

        val queryResult =
            attendancePersistencePort
                .findSessionAttendancesByQuery(query, myTeamNumber)
                .sortedBy { it.teamNumber }
        val paginatedResult = queryResult.paginate { it.id }
        val totalElements =
            attendancePersistencePort.countSessionAttendancesByQuery(query, myTeamNumber)

        return AttendanceMapper.toSessionAttendancesResponse(
            members = paginatedResult.content,
            onlyMyTeam = query.onlyMyTeam ?: false,
            myTeamNumber = myTeamNumber,
            hasNext = paginatedResult.hasNext,
            nextCursorId = paginatedResult.nextCursorId,
            totalElements = totalElements,
        )
    }

    fun getMemberAttendances(query: GetMemberAttendancesQuery): MemberAttendancesResponse {
        val myTeamNumber =
            query.onlyMyTeam
                ?.let { memberQueryService.getMemberTeamNumber(query.memberId) }

        val queryResult =
            attendancePersistencePort
                .findMemberAttendancesByQuery(query, myTeamNumber)
                .sortedBy { it.teamNumber }
        val paginatedResult = queryResult.paginate { it.id }
        val totalElements =
            attendancePersistencePort.countMemberAttendancesByQuery(query, myTeamNumber)

        return AttendanceMapper.toMemberAttendancesResponse(
            members =
                paginatedResult.content
                    .map { member ->
                        MemberAttendanceResponse(
                            id = member.id,
                            name = member.name,
                            teamNumber = member.teamNumber,
                            part = member.part,
                            attendanceStatus =
                                attendanceGraduationEvaluator.evaluate(
                                    onlineAbsentCount = member.onlineAbsentCount,
                                    offlineAbsentCount = member.offlineAbsentCount,
                                    lateCount = member.lateCount,
                                ),
                        )
                    }.toList(),
            onlyMyTeam = (query.teams?.contains(myTeamNumber) == true) || (query.onlyMyTeam ?: false),
            myTeamNumber = myTeamNumber,
            hasNext = paginatedResult.hasNext,
            nextCursorId = paginatedResult.nextCursorId,
            totalElements = totalElements,
        )
    }

    fun getDetailAttendanceBySession(query: GetDetailAttendanceBySessionQuery): DetailAttendancesBySessionResponse {
        val queryResult = (
            attendancePersistencePort
                .findDetailAttendanceBySession(query)
                ?: throw AttendanceNotFoundException()
        )

        return AttendanceMapper.toDetailAttendanceBySessionResponse(
            queryResult,
            evaluation =
                attendanceGraduationEvaluator.evaluate(
                    onlineAbsentCount = queryResult.onlineAbsentCount,
                    offlineAbsentCount = queryResult.offlineAbsentCount,
                    lateCount = queryResult.lateCount,
                ),
        )
    }

    fun getDetailMemberAttendances(query: GetDetailMemberAttendancesQuery): DetailMemberAttendancesResponse {
        val memberAttendanceQueryResult =
            attendancePersistencePort
                .findDetailMemberAttendance(query)
                ?: throw AttendanceNotFoundException()

        val sessionAttendanceQueryResult =
            attendancePersistencePort
                .findMemberSessionAttendances(query)

        return AttendanceMapper.toDetailMemberAttendancesResponse(
            memberAttendanceModel = memberAttendanceQueryResult,
            sessionAttendancesModel = sessionAttendanceQueryResult,
            evaluation =
                attendanceGraduationEvaluator.evaluate(
                    onlineAbsentCount = memberAttendanceQueryResult.onlineAbsentCount,
                    offlineAbsentCount = memberAttendanceQueryResult.offlineAbsentCount,
                    lateCount = memberAttendanceQueryResult.lateCount,
                ),
        )
    }

    fun getMyDetailAttendanceBySession(query: GetMyAttendanceBySessionQuery): MyDetailAttendanceBySessionResponse {
        val myAttendanceQueryResult =
            attendancePersistencePort
                .findMyDetailAttendanceBySession(query)
                ?: throw AttendanceNotFoundException()

        return AttendanceMapper.toMyDetailAttendanceBySessionResponse(myAttendanceQueryResult)
    }

    fun getAttendancesBySessionId(sessionId: SessionId): List<Attendance> =
        attendancePersistencePort.findAllBySessionId(sessionId.value)
}
