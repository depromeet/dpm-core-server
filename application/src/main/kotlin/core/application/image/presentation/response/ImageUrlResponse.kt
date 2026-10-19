package core.application.image.presentation.response

import java.time.Instant

class ImageUrlResponse(
    /** 이미지 원본 읽기 URL. 가진 사람은 만료 전까지 누구나 읽을 수 있다. */
    val url: String,
    val expiresAt: Instant,
) {
    override fun toString(): String = "ImageUrlResponse(expiresAt=$expiresAt)"
}
