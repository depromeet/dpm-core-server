package core.application.image.application.exception

import core.application.common.exception.BusinessException

/** 업로드 세션 상태로 인한 실패(없음, 아직 안 올림, 만료, 저장 실패). 남의 세션도 없음과 같은 404 다. */
class ImageUploadException(
    code: ImageExceptionCode,
) : BusinessException(code)
