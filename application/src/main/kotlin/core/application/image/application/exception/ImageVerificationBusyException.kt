package core.application.image.application.exception

import core.application.common.exception.BusinessException

/** 인스턴스당 동시 검증 1건을 넘었다. 대기열 없이 429 + Retry-After 로 돌려보낸다. */
class ImageVerificationBusyException : BusinessException(ImageExceptionCode.VERIFICATION_BUSY) {
    companion object {
        const val RETRY_AFTER_SECONDS = 3L
    }
}
