package core.application.session.presentation.response

import core.domain.session.enums.SessionAttendanceStatus
import core.domain.session.vo.SessionId
import java.time.LocalDateTime

/** 세션 일시, ID 오름차순. 이전/다음 세션 이동은 이 순서를 쓴다. */
data class SessionSelectorResponse(
    val sessions: List<SessionSelectorItemResponse>,
)

data class SessionSelectorItemResponse(
    val id: SessionId,
    /** 표시용. 정렬에는 쓰지 않는다 */
    val week: Int,
    val date: LocalDateTime,
    val eventName: String,
    val place: String,
    val isOnline: Boolean,
    /** 조회 시각 기준 출석 인증 진행 상태 */
    val attendanceStatus: SessionAttendanceStatus,
)
