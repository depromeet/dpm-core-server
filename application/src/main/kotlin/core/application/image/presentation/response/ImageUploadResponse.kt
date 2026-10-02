package core.application.image.presentation.response

data class ImageUploadResponse(
    val imageId: Long,
    val contentType: String,
    val size: Long,
)
