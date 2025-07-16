package com.server.dpmcore.attendance.domain.port.outbound

import com.server.dpmcore.attendance.application.query.model.MemberAttendanceQueryModel
import com.server.dpmcore.attendance.application.query.model.SessionAttendanceQueryModel
import com.server.dpmcore.attendance.domain.model.Attendance
import com.server.dpmcore.attendance.domain.port.inbound.query.GetAttendancesBySessionIdQuery
import com.server.dpmcore.attendance.domain.port.inbound.query.GetMemberAttendancesQuery
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.session.domain.model.SessionId

interface AttendancePersistencePort {
    fun findAttendanceBy(
        sessionId: SessionId,
        memberId: MemberId,
    ): Attendance

    fun save(attendance: Attendance)

    fun findSessionAttendancesByQuery(query: GetAttendancesBySessionIdQuery): List<SessionAttendanceQueryModel>

    fun findMemberAttendancesByQuery(query: GetMemberAttendancesQuery): List<MemberAttendanceQueryModel>
}
