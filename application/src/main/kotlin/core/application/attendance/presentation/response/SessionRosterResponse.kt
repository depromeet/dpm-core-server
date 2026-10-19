package core.application.attendance.presentation.response

import java.time.LocalDateTime

/**
 * 운영진 세션 출석 명단. 필터 없이 전체 명단을 주고, 이름/상태/팀/파트/내 팀 필터와
 * 상태별 인원은 프론트에서 이 명단으로 계산한다. 팀 선택지는 GET /v3/cohorts/current/teams 를 쓴다.
 */
data class SessionRosterResponse(
    val members: List<SessionRosterMemberResponse>,
    /** 조회한 운영진의 현재 기수 팀 번호. 팀이 없으면 null */
    val myTeamNumber: Int?,
) {
    /** 명단 전체 인원(결석·미인증 포함). members 에서 계산해 따로 저장하지 않는다 */
    val totalElements: Int
        get() = members.size
}

data class SessionRosterMemberResponse(
    val id: Long,
    val name: String,
    /** 현재 기수 팀 번호. 팀이 없으면 null */
    val teamNumber: Int?,
    val isAdmin: Boolean,
    val part: String?,
    val attendanceStatus: String,
    /** 출석 인증 시각. 인증하지 않았거나 운영진이 상태를 바꿨으면 null */
    val attendedAt: LocalDateTime?,
    /** 운영진이 상태를 바꾼 기록이면 true */
    val isManuallyUpdated: Boolean,
    /** 이 세션에 제출한 가장 최근 결석 사유서 내용. 없으면 null */
    val absenceReason: String?,
)
