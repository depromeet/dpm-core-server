package core.domain.session.event

import core.domain.session.vo.SessionId
import core.domain.cohort.vo.CohortId

data class SessionCreateEvent(
    val sessionId: SessionId,
    val cohortId: CohortId,
)
