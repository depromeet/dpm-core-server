package core.application.common.logging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * 장애 한 번에 같은 예외가 초당 수십 건 쏟아지면 채널이 마비되고 웹훅이 429 를 돌려준다.
 * 눌린 건수를 세어 다음 전송에 합쳐 보내는 것까지가 이 클래스의 책임이다.
 */
class ErrorAlertThrottlerTest {
    @Test
    fun `첫 알림은 통과시킨다`() {
        val throttler = ErrorAlertThrottler(cooldownMillis = 1_000, maxPerMinute = 10)

        val decision = throttler.tryAcquire("signature", nowMillis = 0)

        assertThat(decision).isEqualTo(ErrorAlertThrottler.Decision.Send(suppressedCount = 0))
    }

    @Test
    fun `쿨다운 안에 들어온 같은 에러는 막는다`() {
        val throttler = ErrorAlertThrottler(cooldownMillis = 1_000, maxPerMinute = 10)
        throttler.tryAcquire("signature", nowMillis = 0)

        val decision = throttler.tryAcquire("signature", nowMillis = 999)

        assertThat(decision).isEqualTo(ErrorAlertThrottler.Decision.Skip)
    }

    @Test
    fun `쿨다운이 지나면 눌러둔 건수를 함께 알린다`() {
        val throttler = ErrorAlertThrottler(cooldownMillis = 1_000, maxPerMinute = 10)
        throttler.tryAcquire("signature", nowMillis = 0)
        repeat(3) { throttler.tryAcquire("signature", nowMillis = 100L * it) }

        val decision = throttler.tryAcquire("signature", nowMillis = 1_000)

        assertThat(decision).isEqualTo(ErrorAlertThrottler.Decision.Send(suppressedCount = 3))
    }

    @Test
    fun `눌러둔 건수는 한 번 보내면 초기화된다`() {
        val throttler = ErrorAlertThrottler(cooldownMillis = 1_000, maxPerMinute = 10)
        throttler.tryAcquire("signature", nowMillis = 0)
        throttler.tryAcquire("signature", nowMillis = 500)
        throttler.tryAcquire("signature", nowMillis = 1_000)

        val decision = throttler.tryAcquire("signature", nowMillis = 2_000)

        assertThat(decision).isEqualTo(ErrorAlertThrottler.Decision.Send(suppressedCount = 0))
    }

    @Test
    fun `서로 다른 에러가 동시에 터져도 분당 총량을 넘기지 않는다`() {
        val throttler = ErrorAlertThrottler(cooldownMillis = 1_000, maxPerMinute = 2)

        assertThat(throttler.tryAcquire("a", nowMillis = 0)).isInstanceOf(ErrorAlertThrottler.Decision.Send::class.java)
        assertThat(throttler.tryAcquire("b", nowMillis = 1)).isInstanceOf(ErrorAlertThrottler.Decision.Send::class.java)
        assertThat(throttler.tryAcquire("c", nowMillis = 2)).isEqualTo(ErrorAlertThrottler.Decision.Skip)
    }

    @Test
    fun `1분이 지나면 총량 제한이 풀린다`() {
        val throttler = ErrorAlertThrottler(cooldownMillis = 1_000, maxPerMinute = 1)
        throttler.tryAcquire("a", nowMillis = 0)
        assertThat(throttler.tryAcquire("b", nowMillis = 1)).isEqualTo(ErrorAlertThrottler.Decision.Skip)

        val decision = throttler.tryAcquire("b", nowMillis = 60_000)

        assertThat(decision).isEqualTo(ErrorAlertThrottler.Decision.Send(suppressedCount = 1))
    }

    @Test
    fun `추적 대상이 상한을 넘어도 메모리가 무한히 늘지 않는다`() {
        val throttler = ErrorAlertThrottler(cooldownMillis = 60_000, maxPerMinute = 10_000, maxTrackedSignatures = 10)

        repeat(1_000) { throttler.tryAcquire("signature-$it", nowMillis = it.toLong()) }

        // 가장 오래된 시그니처는 밀려났으므로 쿨다운 안이어도 다시 통과한다
        assertThat(throttler.tryAcquire("signature-0", nowMillis = 1_000))
            .isInstanceOf(ErrorAlertThrottler.Decision.Send::class.java)
    }
}
