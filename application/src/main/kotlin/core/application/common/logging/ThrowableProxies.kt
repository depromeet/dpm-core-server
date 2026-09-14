package core.application.common.logging

import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.classic.spi.ThrowableProxy

/**
 * 로그 이벤트에 실린 예외 프록시를 다루는 헬퍼.
 *
 * 로깅 이벤트는 예외를 [IThrowableProxy] 로 감싸 들고 있고, 원인 예외는 체인으로 이어진다.
 * 알림 여부 판단과 메시지 조립 양쪽에서 이 체인을 훑어야 하므로 한 곳에 모아둔다.
 */
internal object ThrowableProxies {
    private const val MAX_CAUSE_DEPTH = 5

    /** 예외와 그 원인들을 순서대로 반환한다. 순환 참조에 대비해 깊이를 제한한다. */
    fun chain(proxy: IThrowableProxy?): List<IThrowableProxy> {
        val chain = mutableListOf<IThrowableProxy>()
        var current = proxy
        while (current != null && chain.size < MAX_CAUSE_DEPTH) {
            chain.add(current)
            current = current.cause
        }
        return chain
    }

    /**
     * 프록시가 감싼 실제 [Throwable] 을 꺼낸다.
     *
     * 같은 JVM 안에서 만들어진 이벤트는 항상 [ThrowableProxy] 라 원본을 꺼낼 수 있다.
     * 직렬화되어 넘어온 이벤트는 원본이 없으므로 null 을 반환한다.
     */
    fun throwableOf(proxy: IThrowableProxy): Throwable? = (proxy as? ThrowableProxy)?.throwable
}
