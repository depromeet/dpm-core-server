package core.application.image.presentation.response

import java.time.Instant

class ImageUploadCreateResponse(
    val uploadId: String,
    /** 이 URL 로 파일을 그대로 PUT 한다. URL 을 가진 사람은 만료 전까지 이 업로드 객체에 쓸 수 있다. */
    val uploadUrl: String,
    val expiresAt: Instant,
) {
    override fun toString(): String = "ImageUploadCreateResponse(uploadId=$uploadId, expiresAt=$expiresAt)"
}
