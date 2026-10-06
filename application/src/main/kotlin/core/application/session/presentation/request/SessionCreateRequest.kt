package core.application.session.presentation.request

import java.time.LocalDateTime

data class SessionCreateRequest(
    val name: String,
    val date: LocalDateTime,
    val isOnline: Boolean? = false,
    val place: String?,
    val week: Int,
    val attendanceStart: LocalDateTime? = null,
    val lateStart: LocalDateTime? = null,
    val absentStart: LocalDateTime? = null,
    /** 피드백 받기 (기본 false). true 이면 feedbackStartAt 필수. */
    val feedbackEnabled: Boolean? = false,
    /** 피드백 수집 시작 시각. feedbackEnabled=true 일 때 필수, 현재 이후여야 한다. */
    val feedbackStartAt: LocalDateTime? = null,
    /** 피드백 알림 보내기 (기본 true). feedbackEnabled=false 면 무시된다. */
    val feedbackPushEnabled: Boolean? = true,
)
