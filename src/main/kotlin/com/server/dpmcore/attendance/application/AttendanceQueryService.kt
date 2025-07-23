package com.server.dpmcore.attendance.application

import com.server.dpmcore.attendance.domain.exception.AttendanceNotFoundException
import com.server.dpmcore.attendance.domain.port.inbound.query.GetAttendancesBySessionWeekQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetDetailAttendanceBySessionQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetDetailMemberAttendancesQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetMemberAttendancesQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetMyAttendanceBySessionQuery
import com.server.dpmcore.attendance.domain.port.outbound.AttendancePersistencePort
import com.server.dpmcore.attendance.presentation.dto.response.DetailAttendancesBySessionResponse
import com.server.dpmcore.attendance.presentation.dto.response.DetailMemberAttendancesResponse
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendanceResponse
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendancesResponse
import com.server.dpmcore.attendance.presentation.dto.response.MyDetailAttendanceBySessionResponse
import com.server.dpmcore.attendance.presentation.dto.response.SessionAttendancesResponse
import com.server.dpmcore.attendance.presentation.mapper.AttendanceMapper
import com.server.dpmcore.common.util.paginate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val PAGE_SIZE = 20

@Service
@Transactional(readOnly = true)
class AttendanceQueryService(
    private val attendancePersistencePort: AttendancePersistencePort,
    private val attendanceGraduationEvaluator: AttendanceGraduationEvaluator,
) {
    fun getAttendancesBySession(query: GetAttendancesBySessionWeekQuery): SessionAttendancesResponse {
        val queryResult =
            attendancePersistencePort
                .findSessionAttendancesByQuery(query)
                .sortedBy { it.teamNumber }
        val paginatedResult = queryResult.paginate { it.id }

        return AttendanceMapper.toSessionAttendancesResponse(
            members = paginatedResult.content,
            hasNext = paginatedResult.hasNext,
            nextCursorId = paginatedResult.nextCursorId,
        )
    }

    fun getMemberAttendances(query: GetMemberAttendancesQuery): MemberAttendancesResponse {
        val queryResult =
            attendancePersistencePort
                .findMemberAttendancesByQuery(query)
                .sortedBy { it.teamNumber }
        val paginatedResult = queryResult.paginate { it.id }

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
            hasNext = paginatedResult.hasNext,
            nextCursorId = paginatedResult.nextCursorId,
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
}
