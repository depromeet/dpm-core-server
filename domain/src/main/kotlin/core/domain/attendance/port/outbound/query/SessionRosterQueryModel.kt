package core.domain.attendance.port.outbound.query

import java.time.Instant

/** 운영진 세션 출석 명단의 한 행. 현재 기수 소속이고 삭제되지 않은 멤버의 살아 있는 출석 기록 하나에 대응한다. */
data class SessionRosterQueryModel(
    val memberId: Long,
    val name: String,
    /** 현재 기수에서 가장 최근 배정된 팀 번호. 팀이 없으면 null */
    val teamNumber: Int?,
    val isAdmin: Boolean,
    val part: String?,
    val attendanceStatus: String,
    /** 저장된 출석 인증 시각. 운영진 변경 뒤에도 그대로 남아 있다 */
    val attendedAt: Instant?,
    /** 운영진 변경 시각. 운영진만 기록한다 */
    val updatedAt: Instant?,
    /** 이 세션에 제출한 가장 최근 결석 사유서 내용. 없으면 null */
    val absenceReason: String?,
)
