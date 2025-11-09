package core.application.session.presentation.response

import core.domain.session.vo.SessionId
import java.time.LocalDateTime

data class SessionWeeksResponse(
    val sessions: List<SessionWeekResponse>,
)

data class SessionWeekResponse(
    val id: SessionId,
    val week: Int,
    val date: LocalDateTime
)
