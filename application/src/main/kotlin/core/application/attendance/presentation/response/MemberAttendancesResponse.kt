package core.application.attendance.presentation.response

data class MemberAttendancesResponse(
    val members: List<MemberAttendanceResponse>,
    /** 조회한 운영진의 현재 기수 최신 배정 팀 번호. 없으면 null */
    val myTeamNumber: Int?,
    /** 현재 기수에 소속되고 삭제되지 않은 멤버 전체 수. teams 필터와 무관하다. */
    val totalElements: Int,
)
