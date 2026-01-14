package core.domain.attendance.port.inbound.query

import core.domain.member.vo.MemberId

data class GetDetailMemberAttendancesQuery(
    val memberId: MemberId,
)
