package core.domain.image.enums

/**
 * 업로드를 허용하는 이미지 형식. 값은 파일 내용(시그니처)으로 판별한 결과이며 확장자나 요청 헤더를 믿지 않는다.
 */
enum class ImageContentType(
    val mimeType: String,
    val extension: String,
) {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    ;

    companion object {
        fun fromMimeType(mimeType: String): ImageContentType =
            fromMimeTypeOrNull(mimeType) ?: throw IllegalArgumentException("지원하지 않는 이미지 형식입니다: $mimeType")

        fun fromMimeTypeOrNull(mimeType: String?): ImageContentType? = entries.firstOrNull { it.mimeType == mimeType }
    }
}
