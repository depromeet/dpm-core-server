package core.application.session.presentation.request

import java.time.LocalDateTime

data class SessionUpdateRequest(
    val sessionId: Long,
    val name: String,
    val date: LocalDateTime,
    val isOnline: Boolean? = false,
    val place: String?,
    val week: Int,
    val attendanceStart: LocalDateTime,
    val lateStart: LocalDateTime,
    val absentStart: LocalDateTime,
    /** 피드백 받기. false 로 바꾸면 설문이 비활성화되고 수집 시작 뒤에는 변경할 수 없다. */
    val feedbackEnabled: Boolean? = false,
    /** 피드백 수집 시작 시각. feedbackEnabled=true 일 때 필수, 변경 시 현재 이후여야 한다. */
    val feedbackStartAt: LocalDateTime? = null,
    /** 피드백 알림 보내기. 수집 시작 전/후 언제든 바꿀 수 있다. */
    val feedbackPushEnabled: Boolean? = true,
)
