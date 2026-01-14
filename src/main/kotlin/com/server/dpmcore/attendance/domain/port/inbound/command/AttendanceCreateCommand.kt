package com.server.dpmcore.attendance.domain.port.inbound.command

import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.session.domain.model.SessionId

data class AttendanceCreateCommand(
    val sessionId: SessionId,
    val memberId: MemberId,
)
