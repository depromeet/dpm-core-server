package core.domain.session.event

import core.domain.cohort.vo.CohortId
import core.domain.session.vo.SessionId

data class SessionCreateEvent(
    val sessionId: SessionId,
    val cohortId: CohortId,
)
