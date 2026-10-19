package core.application.member.application.service

import core.domain.image.port.outbound.ImageStoragePort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/** DB 삭제가 커밋된 뒤에만 버킷 객체를 지운다. 실패해도 DB는 이미 정리됐으므로 로그만 남긴다. */
@Component
class MemberPurgedEventListener(
    private val imageStoragePort: ImageStoragePort,
) {
    private val logger = KotlinLogging.logger { MemberPurgedEventListener::class.java }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun deleteImageObjects(event: MemberPurgedEvent) {
        event.imageObjectKeys.forEach { objectKey ->
            runCatching { imageStoragePort.delete(objectKey) }
                .onFailure {
                    logger.error(it) { "Failed to delete image object of purged member ${event.memberId}: $objectKey" }
                }
        }
        logger.info { "Purged member ${event.memberId} with ${event.imageObjectKeys.size} image objects" }
    }
}
