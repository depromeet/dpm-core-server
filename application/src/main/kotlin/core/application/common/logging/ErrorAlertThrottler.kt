package core.application.common.logging

/**
 * 같은 에러가 반복될 때 Discord 로 나가는 양을 제한한다.
 *
 * 장애 한 번에 같은 예외가 초당 수십 건씩 쌓이면 채널이 마비되고 웹훅이 429 를 돌려준다.
 * 시그니처별 쿨다운으로 중복을 누르고, 서로 다른 에러가 동시에 터지는 경우를 대비해 분당 총량도 함께 막는다.
 * 눌린 건수는 버리지 않고 다음 전송에 "외 N건" 으로 합쳐 보낸다.
 */
class ErrorAlertThrottler(
    private val cooldownMillis: Long,
    private val maxPerMinute: Int,
    private val maxTrackedSignatures: Int = DEFAULT_MAX_TRACKED_SIGNATURES,
) {
    private val lastSentAt =
        object : LinkedHashMap<String, Long>(INITIAL_CAPACITY, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>): Boolean =
                size > maxTrackedSignatures
        }
    private val suppressedCounts = HashMap<String, Int>()
    private val recentSentAt = ArrayDeque<Long>()

    @Synchronized
    fun tryAcquire(
        signature: String,
        nowMillis: Long,
    ): Decision {
        val lastSent = lastSentAt[signature]
        if (lastSent != null && nowMillis - lastSent < cooldownMillis) {
            return suppress(signature)
        }

        evictOutOfWindow(nowMillis)
        if (recentSentAt.size >= maxPerMinute) {
            return suppress(signature)
        }

        recentSentAt.addLast(nowMillis)
        lastSentAt[signature] = nowMillis
        val suppressed = suppressedCounts.remove(signature) ?: 0
        return Decision.Send(suppressed)
    }

    private fun suppress(signature: String): Decision {
        if (suppressedCounts.size < maxTrackedSignatures) {
            suppressedCounts.merge(signature, 1, Int::plus)
        }
        return Decision.Skip
    }

    private fun evictOutOfWindow(nowMillis: Long) {
        while (recentSentAt.isNotEmpty() && nowMillis - recentSentAt.first() >= WINDOW_MILLIS) {
            recentSentAt.removeFirst()
        }
    }

    sealed interface Decision {
        /** 전송한다. [suppressedCount] 는 쿨다운에 눌려 보내지 못했던 같은 에러의 건수다. */
        data class Send(
            val suppressedCount: Int,
        ) : Decision

        /** 쿨다운 또는 분당 총량에 걸려 보내지 않는다. */
        data object Skip : Decision
    }

    companion object {
        private const val WINDOW_MILLIS = 60_000L
        private const val DEFAULT_MAX_TRACKED_SIGNATURES = 500
        private const val INITIAL_CAPACITY = 64
        private const val LOAD_FACTOR = 0.75f
    }
}
