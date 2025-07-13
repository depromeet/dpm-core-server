package com.server.dpmcore.attendance.application

import com.server.dpmcore.attendance.domain.port.inbound.query.GetAttendancesBySessionIdQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetMemberAttendancesQuery
import com.server.dpmcore.attendance.infrastructure.repository.AttendanceRepository
import com.server.dpmcore.attendance.presentation.dto.response.MemberAttendancesResponse
import com.server.dpmcore.attendance.presentation.dto.response.SessionAttendancesResponse
import com.server.dpmcore.attendance.presentation.mapper.AttendanceMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val PAGE_SIZE = 20

@Service
@Transactional(readOnly = true)
class AttendanceQueryService(
    private val attendanceRepository: AttendanceRepository,
) {
    fun getAttendancesBySession(query: GetAttendancesBySessionIdQuery): SessionAttendancesResponse {
        var results =
            attendanceRepository
                .findSessionAttendencesByQuery(query)
                .sortedBy { it.teamNumber }

        var hasNext = false
        var nextCursorId: Long? = null
        if (results.isNotEmpty() && results.size > PAGE_SIZE) {
            hasNext = true
            nextCursorId = results.last().id
            results = results.take(PAGE_SIZE)
        }

        return AttendanceMapper.toSessionAttendancesResponse(
            members = results,
            hasNext = hasNext,
            nextCursorId = nextCursorId,
        )
    }

    fun getMemberAttendances(query: GetMemberAttendancesQuery): MemberAttendancesResponse {
        var results =
            attendanceRepository
                .findMemberAttendancesByQuery(query)
                .sortedBy { it.teamNumber }

        var hasNext = false
        var nextCursorId: Long? = null
        if (results.isNotEmpty() && results.size > PAGE_SIZE) {
            hasNext = true
            nextCursorId = results.last().id
            results = results.take(PAGE_SIZE)
        }

        return AttendanceMapper.toMemberAttendancesResponse(
            members = results,
            hasNext = hasNext,
            nextCursorId = nextCursorId,
        )
    }
}
