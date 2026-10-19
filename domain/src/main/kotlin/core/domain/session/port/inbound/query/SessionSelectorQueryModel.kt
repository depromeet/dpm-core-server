package core.domain.session.port.inbound.query

import core.domain.session.enums.SessionAttendanceStatus
import core.domain.session.vo.SessionId
import java.time.Instant

/** 운영진 세션 선택 목록의 한 세션 */
data class SessionSelectorQueryModel(
    val sessionId: SessionId,
    val week: Int,
    val date: Instant,
    val eventName: String,
    val place: String,
    val isOnline: Boolean,
    /** 조회 시각 기준 출석 인증 진행 상태 */
    val attendanceStatus: SessionAttendanceStatus,
)
