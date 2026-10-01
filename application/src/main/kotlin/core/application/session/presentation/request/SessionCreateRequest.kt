package core.application.session.presentation.request

import java.time.LocalDateTime

/**
 * 세션 생성 요청.
 *
 * attendanceStart/lateStart/absentStart 를 모두 생략하면 서버 설정 기본 출석 시간(기본 T-10/T+15/T+30분)으로 계산하고,
 * 모두 입력하면 이 세션만의 명시적인 시각으로 사용한다. 일부만 입력하면 400(SESSION-400-09)이다.
 */
data class SessionCreateRequest(
    val name: String,
    val date: LocalDateTime,
    val isOnline: Boolean? = false,
    val place: String?,
    val week: Int,
    val attendanceStart: LocalDateTime? = null,
    val lateStart: LocalDateTime? = null,
    val absentStart: LocalDateTime? = null,
)
