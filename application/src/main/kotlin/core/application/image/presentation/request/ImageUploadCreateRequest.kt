package core.application.image.presentation.request

data class ImageUploadCreateRequest(
    /** image/jpeg 또는 image/png. 프론트는 PUT 할 때 같은 값을 Content-Type 으로 보내야 한다. */
    val contentType: String,
    /** 올릴 파일의 바이트 수. 실제 업로드 크기와 같아야 한다. */
    val size: Long,
)
