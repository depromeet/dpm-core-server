package core.domain.attendance.port.inbound.query

import core.domain.member.vo.MemberId

data class GetMemberAttendancesQuery(
    val memberId: MemberId,
    /** 현재 기수 최신 배정 팀 번호 필터. 없거나 비면 전체 */
    val teams: List<Int>?,
)
