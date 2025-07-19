package com.server.dpmcore.attendance.domain.port.inbound.query

import com.server.dpmcore.member.member.domain.model.MemberId

data class GetDetailMemberAttendancesQuery(
    val memberId: MemberId,
)
