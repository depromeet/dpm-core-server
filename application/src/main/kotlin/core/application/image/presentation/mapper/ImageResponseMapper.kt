package core.application.image.presentation.mapper

import core.application.image.application.dto.ImageContent
import org.springframework.http.CacheControl
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.util.UUID

object ImageResponseMapper {
    /** 비공개 이미지 원본 응답. 캐시하지 않고 내용 추측을 막는다. */
    fun toInlineResponse(image: ImageContent): ResponseEntity<ByteArray> {
        // 저장소 키나 원본 파일명을 드러내지 않도록 응답마다 새 UUID 파일명을 쓴다.
        val filename = "${UUID.randomUUID()}.${image.contentType.extension}"
        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(image.contentType.mimeType))
            .contentLength(image.bytes.size.toLong())
            .cacheControl(CacheControl.noStore().cachePrivate())
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(filename).build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(image.bytes)
    }
}
