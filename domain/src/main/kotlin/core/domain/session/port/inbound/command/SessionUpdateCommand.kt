package core.domain.session.port.inbound.command

import core.domain.session.vo.SessionId
import java.time.Instant

data class SessionUpdateCommand(
    val sessionId: SessionId,
    val date: Instant,
    val week: Int,
    val place: String?,
    val eventName: String?,
    val isOnline: Boolean?,
    val attendanceStart: Instant,
    val lateStart: Instant,
    val absentStart: Instant,
    /** 피드백 받기. false 로 바꾸면 설문이 비활성화되고 수집 시작 뒤에는 변경할 수 없다. */
    val feedbackEnabled: Boolean = false,
    /** 피드백 수집 시작 시각. [feedbackEnabled] 가 true 일 때만 사용된다. */
    val feedbackStartAt: Instant? = null,
    /** 피드백 알림 보내기. 수집 시작 전/후 언제든 변경 가능. */
    val feedbackPushEnabled: Boolean = true,
)
