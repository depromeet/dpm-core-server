package core.domain.session.event

import core.domain.session.vo.SessionId
import java.time.Instant

/** [lateStart], [absentStart] 는 발행 시점 값이다. 처리하는 쪽은 세션의 최신 값을 다시 읽어 써야 한다. */
data class SessionUpdateEvent(
    val sessionId: SessionId,
    val lateStart: Instant,
    val absentStart: Instant,
)
