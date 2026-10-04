package core.application.image.application.validator

import core.application.image.application.exception.ImageExceptionCode
import core.application.image.application.exception.InvalidImageException
import core.domain.image.enums.ImageContentType
import org.springframework.http.InvalidMediaTypeException
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.imageio.stream.FileImageInputStream
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * 업로드 이미지 검증. 형식은 파일 시그니처로만 판별하고(확장자 무시), 선언된 Content-Type 이 있으면 일치해야 한다.
 * 헤더의 크기를 먼저 확인해 픽셀 수 상한을 넘으면 디코드하지 않으며, 디코드는 subsampling 으로 메모리를 제한한다.
 * 파일 전체를 메모리에 올리지 않도록 임시 파일을 직접 읽는다(ImageIO 캐시도 쓰지 않음).
 */
@Component
class ImageValidator {
    fun validate(
        file: Path,
        declaredContentType: String?,
    ): ImageContentType {
        val size = Files.size(file)
        if (size == 0L) throw InvalidImageException(ImageExceptionCode.EMPTY_FILE)
        if (size > MAX_BYTES) throw InvalidImageException(ImageExceptionCode.FILE_TOO_LARGE)

        val header = Files.newInputStream(file).use { it.readNBytes(PNG_SIGNATURE.size) }
        val detected = detect(header) ?: throw InvalidImageException(ImageExceptionCode.UNSUPPORTED_TYPE)
        if (!declaredContentType.isNullOrBlank() && normalize(declaredContentType) != detected.mimeType) {
            throw InvalidImageException(ImageExceptionCode.CONTENT_TYPE_MISMATCH)
        }
        decode(file, detected)
        return detected
    }

    /** 요청의 Content-Type 을 허용 형식으로 바꾼다. 파라미터(charset 등)와 대소문자는 무시한다. */
    fun parseContentType(contentType: String?): ImageContentType? =
        contentType?.let { ImageContentType.fromMimeTypeOrNull(normalize(it)) }

    private fun detect(header: ByteArray): ImageContentType? =
        when {
            header.startsWith(JPEG_SIGNATURE) -> ImageContentType.JPEG
            header.startsWith(PNG_SIGNATURE) -> ImageContentType.PNG
            else -> null
        }

    // 파라미터는 보지 않는다. charset=binary 처럼 모르는 charset 이 있으면 Spring 파서가 통째로 거절하므로 먼저 뗀다.
    private fun normalize(contentType: String): String? =
        try {
            MediaType.parseMediaType(contentType.substringBefore(';')).let { "${it.type}/${it.subtype}".lowercase() }
        } catch (e: InvalidMediaTypeException) {
            null
        }

    private fun decode(
        file: Path,
        type: ImageContentType,
    ) {
        val formatName = if (type == ImageContentType.JPEG) "jpeg" else "png"
        val reader =
            ImageIO.getImageReadersByFormatName(formatName).asSequence().firstOrNull()
                ?: throw IllegalStateException("ImageIO reader 가 없습니다: $formatName")
        try {
            FileImageInputStream(file.toFile()).use { input ->
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
