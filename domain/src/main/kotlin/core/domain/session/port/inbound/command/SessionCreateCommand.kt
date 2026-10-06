package core.domain.session.port.inbound.command

import java.time.Instant

/** 출석 시각은 모두 생략(서버 기본값)하거나 모두 제공해야 한다. */
data class SessionCreateCommand(
    val date: Instant,
    val week: Int,
    val place: String?,
    val eventName: String?,
    val isOnline: Boolean?,
    val attendanceStart: Instant? = null,
    val lateStart: Instant? = null,
    val absentStart: Instant? = null,
    /** 피드백 받기. true 이면 [feedbackStartAt] 필수. */
    val feedbackEnabled: Boolean = false,
    /** 피드백 수집 시작 시각. [feedbackEnabled] 가 true 일 때만 사용된다. */
    val feedbackStartAt: Instant? = null,
    /** 피드백 알림 보내기 (기본 true). */
    val feedbackPushEnabled: Boolean = true,
)
