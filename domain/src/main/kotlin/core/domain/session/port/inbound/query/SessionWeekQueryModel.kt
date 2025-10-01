package core.domain.session.port.inbound.query

import core.domain.session.vo.SessionId

data class SessionWeekQueryModel(
    val sessionId: SessionId,
    val week: Int,
)
