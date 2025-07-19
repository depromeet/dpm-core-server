package com.server.dpmcore.attendance.application

import com.server.dpmcore.attendance.domain.exception.AttendanceNotFoundException
import com.server.dpmcore.attendance.domain.port.inbound.query.GetAttendancesBySessionIdQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetDetailAttendanceBySessionQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetMemberAttendancesQuery
import com.server.dpmcore.attendance.infrastructure.repository.AttendanceRepository
import com.server.dpmcore.attendance.presentation.dto.response.DetailAttendanceBySessionResponse
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendanceResponse
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendancesResponse
import com.server.dpmcore.attendance.presentation.dto.response.SessionAttendancesResponse
import com.server.dpmcore.attendance.presentation.mapper.AttendanceMapper
import com.server.dpmcore.common.util.paginate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val PAGE_SIZE = 20

@Service
@Transactional(readOnly = true)
class AttendanceQueryService(
    private val attendanceRepository: AttendanceRepository,
    private val attendanceGraduationEvaluator: AttendanceGraduationEvaluator,
) {
    fun getAttendancesBySession(query: GetAttendancesBySessionIdQuery): SessionAttendancesResponse {
        val results =
            attendanceRepository
                .findSessionAttendancesByQuery(query)
                .sortedBy { it.teamNumber }
        val paginatedResult = results.paginate { it.id }

        return AttendanceMapper.toSessionAttendancesResponse(
            members = paginatedResult.content,
            hasNext = paginatedResult.hasNext,
            nextCursorId = paginatedResult.nextCursorId,
        )
    }

    fun getMemberAttendances(query: GetMemberAttendancesQuery): MemberAttendancesResponse {
        val results =
            attendanceRepository
                .findMemberAttendancesByQuery(query)
                .sortedBy { it.teamNumber }
        val paginatedResult = results.paginate { it.id }

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

    fun getDetailAttendanceBySession(query: GetDetailAttendanceBySessionQuery): DetailAttendanceBySessionResponse {
        val result = (
            attendanceRepository
                .findDetailAttendanceBySession(query)
                ?: throw AttendanceNotFoundException()
        )

        return AttendanceMapper.toDetailAttendanceBySessionResponse(
            result,
            attendanceStatus =
                attendanceGraduationEvaluator.evaluate(
                    onlineAbsentCount = result.onlineAbsentCount,
                    offlineAbsentCount = result.offlineAbsentCount,
                    lateCount = result.lateCount,
                ),
        )
    }
}
