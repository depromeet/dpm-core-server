package core.application.session.presentation.response

import core.domain.session.vo.SessionId

data class SessionWeeksResponse(
    val sessions: List<SessionWeekResponse>,
)

data class SessionWeekResponse(
    val id: SessionId,
    val week: Int,
)
