package core.application.image.application.exception

import core.application.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class ImageExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "IMAGE-400-01", "업로드할 파일이 비어 있습니다"),
    CONTENT_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "IMAGE-400-02", "파일 내용과 Content-Type 이 일치하지 않습니다"),
    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "IMAGE-400-03", "이미지를 읽을 수 없습니다"),
    DIMENSIONS_TOO_LARGE(HttpStatus.BAD_REQUEST, "IMAGE-400-04", "이미지 해상도가 너무 큽니다 (최대 2,500만 픽셀)"),
    SIZE_MISMATCH(HttpStatus.BAD_REQUEST, "IMAGE-400-05", "업로드한 파일 크기가 요청한 크기와 다릅니다"),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMAGE-404-01", "이미지를 찾을 수 없습니다"),
    UPLOAD_NOT_FOUND(HttpStatus.NOT_FOUND, "IMAGE-404-02", "업로드를 찾을 수 없습니다"),
    NOT_UPLOADED(HttpStatus.CONFLICT, "IMAGE-409-01", "업로드된 파일이 없습니다. 업로드 URL 로 파일을 올린 뒤 다시 요청해주세요"),
    SOURCE_CHANGED(HttpStatus.CONFLICT, "IMAGE-409-02", "검증 이후 파일이 바뀌었습니다. 다시 업로드해주세요"),
    UPLOAD_FAILED(HttpStatus.CONFLICT, "IMAGE-409-03", "이미지를 저장하지 못했습니다. 다시 업로드해주세요"),
    UPLOAD_EXPIRED(HttpStatus.GONE, "IMAGE-410-01", "업로드 URL 이 만료되었습니다. 다시 업로드해주세요"),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE-413-01", "이미지는 10MiB 이하만 업로드할 수 있습니다"),
    UNSUPPORTED_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "IMAGE-415-01", "JPEG 또는 PNG 이미지만 업로드할 수 있습니다"),
    VERIFICATION_BUSY(HttpStatus.TOO_MANY_REQUESTS, "IMAGE-429-01", "다른 이미지를 검증하고 있습니다. 잠시 후 다시 시도해주세요"),
    STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "IMAGE-503-01", "이미지 저장소를 사용할 수 없습니다. 잠시 후 다시 시도해주세요"),
    ;

    override fun getStatus(): HttpStatus = this.status

    override fun getCode(): String = this.code

    override fun getMessage(): String = this.message
}
