package core.application.image.presentation.response

data class ImageUploadResponse(
    val imageId: Long,
    val contentType: String,
    val size: Long,
    /** 업로드 요청에 보낸 원본 파일명. 보내지 않았으면 null */
    val fileName: String?,
)
