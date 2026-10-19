package core.application.image.application.exception

import core.application.common.exception.BusinessException

class InvalidImageException(
    val imageCode: ImageExceptionCode,
) : BusinessException(imageCode)
