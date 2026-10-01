package core.domain.session.port.inbound.command

import java.time.Instant

/**
 * 세션 생성 명령입니다.
 *
 * 출석 시각 세 개는 모두 생략(null)하면 서버 설정 기본값으로 계산하고,
 * 모두 제공하면 해당 세션만의 명시적인 예외로 그대로 사용합니다. 일부만 제공하는 것은 허용하지 않습니다.
 */
data class SessionCreateCommand(
    val date: Instant,
    val week: Int,
    val place: String?,
    val eventName: String?,
    val isOnline: Boolean?,
    val attendanceStart: Instant? = null,
    val lateStart: Instant? = null,
    val absentStart: Instant? = null,
)
