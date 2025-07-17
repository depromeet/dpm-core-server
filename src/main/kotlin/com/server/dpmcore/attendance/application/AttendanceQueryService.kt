package com.server.dpmcore.attendance.application

import com.server.dpmcore.attendance.application.config.AttendanceEvaluationProperties
import com.server.dpmcore.attendance.domain.port.inbound.query.GetAttendancesBySessionIdQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetMemberAttendancesQuery
import com.server.dpmcore.attendance.infrastructure.repository.AttendanceRepository
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
    private val attendanceEvaluationProperties: AttendanceEvaluationProperties,
) {
    fun getAttendancesBySession(query: GetAttendancesBySessionIdQuery): SessionAttendancesResponse {
        var results =
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
        var results =
            attendanceRepository
                .findMemberAttendancesByQuery(query)
                .sortedBy { it.teamNumber }
        val paginatedResult = results.paginate { it.id }

        return AttendanceMapper.toMemberAttendancesResponse(
            members = paginatedResult.content,
            hasNext = paginatedResult.hasNext,
            nextCursorId = paginatedResult.nextCursorId,
            impossibleThreshold = attendanceEvaluationProperties.impossibleThreshold,
            atRiskThreshold = attendanceEvaluationProperties.atRiskThreshold,
        )
    }
}
