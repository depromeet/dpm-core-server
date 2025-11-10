package core.domain.session.port.inbound.query

import core.domain.session.vo.SessionId
import java.time.Instant

data class SessionWeekQueryModel(
    val sessionId: SessionId,
    val week: Int,
    val date: Instant,
)
