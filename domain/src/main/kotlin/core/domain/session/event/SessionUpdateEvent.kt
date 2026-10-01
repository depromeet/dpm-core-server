package core.domain.session.event

import core.domain.session.vo.SessionId
import java.time.Instant

/**
 * 세션 출석 시각이 바뀌었음을 알리는 이벤트.
 *
 * [lateStart], [absentStart] 는 발행 시점의 참고 값이다. 처리하는 쪽은 이 값으로 상태를 덮어쓰지 말고
 * 세션의 최신 값을 다시 읽어 사용해야 한다(뒤늦게 처리되는 이벤트가 이후 변경을 덮어쓰지 않도록).
 */
data class SessionUpdateEvent(
    val sessionId: SessionId,
    val lateStart: Instant,
    val absentStart: Instant,
)
