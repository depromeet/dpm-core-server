package core.application.refreshToken.application.scheduler

import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 리프레시 토큰 테이블을 주기적으로 정리한다.
 *
 * 이전 스키마에는 만료 개념 자체가 없어 정리가 불가능했고, prod 209행 중 195행이
 * 만료된 채 방치돼 있었다. 회전 이력 행도 재발급마다 쌓이므로 함께 정리한다.
 */
@Component
class RefreshTokenCleanupScheduler(
    private val refreshTokenPersistencePort: RefreshTokenPersistencePort,
) {
    private val logger = KotlinLogging.logger { }

    /** 회전 유예가 지난 이력 행 정리. 상한 계산에서 제외되므로 자주 돌 필요는 없다. */
    @Scheduled(fixedDelay = ROTATED_CLEANUP_INTERVAL_MS, initialDelay = ROTATED_CLEANUP_INTERVAL_MS)
    @Transactional
    fun purgeRotated() {
        val threshold = Instant.now().minusSeconds(ROTATED_RETENTION_SECONDS)
        val deleted = refreshTokenPersistencePort.deleteRotatedBefore(threshold)
        if (deleted > 0) {
            logger.info { "rotated refresh tokens purged: count=$deleted" }
        }
    }

    @Scheduled(cron = EXPIRED_CLEANUP_CRON, zone = CLEANUP_ZONE)
    @Transactional
    fun purgeExpired() {
        val deleted = refreshTokenPersistencePort.deleteExpired(Instant.now())
        if (deleted > 0) {
            logger.info { "expired refresh tokens purged: count=$deleted" }
        }
    }

    companion object {
        private const val EXPIRED_CLEANUP_CRON = "0 30 4 * * *"
        private const val CLEANUP_ZONE = "Asia/Seoul"

        /** 회전 유예(60초)보다 넉넉히 길게 잡아, 유예 중인 행이 지워지지 않게 한다. */
        private const val ROTATED_RETENTION_SECONDS = 600L
        private const val ROTATED_CLEANUP_INTERVAL_MS = 3_600_000L
    }
}
