package com.server.dpmcore.attendance.domain.port.outbound

import com.server.dpmcore.attendance.domain.model.Attendance
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.session.domain.model.SessionId

interface AttendancePersistencePort {
    fun findAttendanceBy(
        sessionId: SessionId,
        memberId: MemberId,
    ): Attendance

    fun save(attendance: Attendance)
}
