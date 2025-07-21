package com.server.dpmcore.attendance.domain.port.inbound.query

import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.session.domain.model.SessionId

data class GetDetailAttendanceBySessionQuery(
    val sessionId: SessionId,
    val memberId: MemberId,
)
