package core.application.image.presentation.request

data class ImageUploadCreateRequest(
    /** image/jpeg 또는 image/png. 프론트는 PUT 할 때 같은 값을 Content-Type 으로 보내야 한다. */
    val contentType: String,
    /** 올릴 파일의 바이트 수. 실제 업로드 크기와 같아야 한다. */
    val size: Long,
    /** 원본 파일명(선택, 255자 이하). 경로가 붙어 있으면 마지막 부분만 저장한다. 표시용이며 저장 키에는 쓰지 않는다. */
    val fileName: String? = null,
)
