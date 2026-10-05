package core.application.image.application.exception

import core.application.common.exception.BusinessException

/** 설정 누락, 인증 실패, 스토리지 오류. 원인은 어댑터에서 로그로만 남기고 응답에는 싣지 않는다. */
class ImageStorageUnavailableException : BusinessException(ImageExceptionCode.STORAGE_UNAVAILABLE)
