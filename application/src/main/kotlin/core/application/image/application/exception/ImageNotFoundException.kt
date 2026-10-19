package core.application.image.application.exception

import core.application.common.exception.BusinessException

/** 없는 이미지와 남의 이미지를 구분하지 않는다(존재 여부 노출 방지). */
class ImageNotFoundException : BusinessException(ImageExceptionCode.IMAGE_NOT_FOUND)
