package core.application.image.application.dto

import core.domain.image.enums.ImageContentType

class ImageContent(
    val contentType: ImageContentType,
    val bytes: ByteArray,
)
