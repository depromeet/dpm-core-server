package core.application.support

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** 테스트에서 현재 시각을 직접 정하는 Clock. */
class MutableClock(
    @Volatile var now: Instant,
) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId?): Clock = this

    override fun instant(): Instant = now
}
