package core.application.image.application.scheduler

import core.application.image.application.service.ImageCommandService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 버려진 업로드 세션과 남은 PAR·업로드 객체를 정리한다. 한 번에 정해진 건수만 보며 남은 건은 다음 실행이 이어서 처리한다.
 * 클라우드 호출이 있어 트랜잭션을 걸지 않는다(상태 변경은 건별 조건부 UPDATE). 여러 인스턴스가 동시에 돌아도 안전하다.
 */
@Component
class ImageUploadCleanupScheduler(
    private val imageCommandService: ImageCommandService,
) {
    private val logger = KotlinLogging.logger { }

    @Scheduled(fixedDelay = CLEANUP_INTERVAL_MS, initialDelay = CLEANUP_INTERVAL_MS)
    fun cleanUpStaleUploads() {
        val cleaned = imageCommandService.cleanUpStaleUploads()
        if (cleaned > 0) {
            logger.info { "stale image uploads cleaned: count=$cleaned" }
        }
    }

    companion object {
        private const val CLEANUP_INTERVAL_MS = 1_800_000L
    }
}
