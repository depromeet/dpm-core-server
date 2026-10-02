package core.application.image.application.validator

import core.application.image.application.exception.ImageExceptionCode
import core.application.image.application.exception.InvalidImageException
import core.domain.image.enums.ImageContentType
import org.springframework.http.InvalidMediaTypeException
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * 업로드 이미지 검증. 형식은 파일 시그니처로만 판별하고(확장자 무시), 선언된 Content-Type 이 있으면 일치해야 한다.
 * 헤더의 크기를 먼저 확인해 픽셀 수 상한을 넘으면 디코드하지 않으며, 디코드는 subsampling 으로 메모리를 제한한다.
 */
@Component
class ImageValidator {
    fun validate(
        content: ByteArray,
        declaredContentType: String?,
    ): ImageContentType {
        if (content.isEmpty()) throw InvalidImageException(ImageExceptionCode.EMPTY_FILE)
        if (content.size > MAX_BYTES) throw InvalidImageException(ImageExceptionCode.FILE_TOO_LARGE)

        val detected = detect(content) ?: throw InvalidImageException(ImageExceptionCode.UNSUPPORTED_TYPE)
        if (!declaredContentType.isNullOrBlank() && normalize(declaredContentType) != detected.mimeType) {
            throw InvalidImageException(ImageExceptionCode.CONTENT_TYPE_MISMATCH)
        }
        decode(content, detected)
        return detected
    }

    private fun detect(content: ByteArray): ImageContentType? =
        when {
            content.startsWith(JPEG_SIGNATURE) -> ImageContentType.JPEG
            content.startsWith(PNG_SIGNATURE) -> ImageContentType.PNG
            else -> null
        }

    private fun normalize(contentType: String): String? =
        try {
            MediaType.parseMediaType(contentType).let { "${it.type}/${it.subtype}".lowercase() }
        } catch (e: InvalidMediaTypeException) {
            null
        }

    private fun decode(
        content: ByteArray,
        type: ImageContentType,
    ) {
        val formatName = if (type == ImageContentType.JPEG) "jpeg" else "png"
        val reader =
            ImageIO.getImageReadersByFormatName(formatName).asSequence().firstOrNull()
                ?: throw IllegalStateException("ImageIO reader 가 없습니다: $formatName")
        try {
            // 임시 파일 캐시를 쓰지 않도록 메모리 스트림을 직접 만든다.
            MemoryCacheImageInputStream(ByteArrayInputStream(content)).use { input ->
                reader.setInput(input, true, true)
                val pixels = reader.getWidth(0).toLong() * reader.getHeight(0)
                if (pixels <= 0) throw InvalidImageException(ImageExceptionCode.INVALID_IMAGE)
                if (pixels > MAX_PIXELS) throw InvalidImageException(ImageExceptionCode.DIMENSIONS_TOO_LARGE)

                val step = ceil(sqrt(pixels.toDouble() / DECODE_PIXEL_BUDGET)).toInt()
                val param = reader.defaultReadParam
                if (step > 1) param.setSourceSubsampling(step, step, 0, 0)
                // 잘린 JPEG 는 예외 없이 경고만 남기므로 경고도 손상으로 본다. 네이티브 콜백이라 리스너에서는 던지지 않는다.
                var warned = false
                reader.addIIOReadWarningListener { _, _ -> warned = true }
                reader.read(0, param)
                if (warned) throw InvalidImageException(ImageExceptionCode.INVALID_IMAGE)
            }
        } catch (e: InvalidImageException) {
            throw e
        } catch (e: IOException) {
            throw InvalidImageException(ImageExceptionCode.INVALID_IMAGE)
        } catch (e: RuntimeException) {
            throw InvalidImageException(ImageExceptionCode.INVALID_IMAGE)
        } finally {
            reader.dispose()
        }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    companion object {
        const val MAX_BYTES = 10 * 1024 * 1024
        const val MAX_PIXELS = 25_000_000L

        // 디코드 결과를 이 픽셀 수 안팎으로 줄여 요청당 힙 사용을 묶는다(4M px ≈ 16MB).
        private const val DECODE_PIXEL_BUDGET = 4_000_000.0
        private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    }
}
