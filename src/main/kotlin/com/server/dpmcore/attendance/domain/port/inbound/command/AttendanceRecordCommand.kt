package com.server.dpmcore.attendance.domain.port.inbound.command

import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.session.domain.model.SessionId
import java.time.Instant

data class AttendanceRecordCommand(
    val sessionId: SessionId,
    val memberId: MemberId,
    val attendedAt: Instant,
    val attendanceCode: String,
)
