package core.application.image.application.validator

import core.application.image.ImageFixtures
import core.application.image.application.exception.ImageExceptionCode
import core.application.image.application.exception.InvalidImageException
import core.domain.image.enums.ImageContentType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ImageValidatorTest {
    private val validator = ImageValidator()

    @Test
    fun `실제 PNG 와 JPEG 는 내용으로 판별된다`() {
        assertThat(validator.validate(ImageFixtures.png(), "image/png")).isEqualTo(ImageContentType.PNG)
        assertThat(validator.validate(ImageFixtures.jpeg(), "image/jpeg")).isEqualTo(ImageContentType.JPEG)
    }

    @Test
    fun `Content-Type 이 없으면 내용만으로 판별한다`() {
        assertThat(validator.validate(ImageFixtures.png(), null)).isEqualTo(ImageContentType.PNG)
    }

    @Test
    fun `선언한 Content-Type 이 내용과 다르면 거절한다`() {
        assertCode(ImageExceptionCode.CONTENT_TYPE_MISMATCH) { validator.validate(ImageFixtures.png(), "image/jpeg") }
        assertCode(ImageExceptionCode.CONTENT_TYPE_MISMATCH) { validator.validate(ImageFixtures.jpeg(), "not a type") }
    }

    @Test
    fun `JPEG PNG 가 아닌 형식은 확장자와 무관하게 거절한다`() {
        val svg = """<svg xmlns="http://www.w3.org/2000/svg"><script>alert(1)</script></svg>""".toByteArray()
        val webp = "RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".toByteArray(Charsets.ISO_8859_1)

        assertCode(ImageExceptionCode.UNSUPPORTED_TYPE) { validator.validate(svg, "image/png") }
        assertCode(ImageExceptionCode.UNSUPPORTED_TYPE) { validator.validate(webp, null) }
        assertCode(ImageExceptionCode.UNSUPPORTED_TYPE) { validator.validate(ImageFixtures.gif(), null) }
    }

    @Test
    fun `시그니처만 맞고 디코드되지 않는 파일은 거절한다`() {
        val png = ImageFixtures.png(64, 64)
        val truncatedPng = png.copyOf(png.size / 2)
        val fakeJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(64) { 7 }
        val jpeg = ImageFixtures.jpeg(256, 256)
        val jpegWithoutEoi = jpeg.copyOf(jpeg.size - 2)

        assertCode(ImageExceptionCode.INVALID_IMAGE) { validator.validate(truncatedPng, null) }
        assertCode(ImageExceptionCode.INVALID_IMAGE) { validator.validate(fakeJpeg, null) }
        assertCode(ImageExceptionCode.INVALID_IMAGE) { validator.validate(jpegWithoutEoi, null) }
    }

    @Test
    fun `헤더의 픽셀 수가 상한을 넘으면 디코드 전에 거절한다`() {
        // 픽셀 데이터가 없어 디코드했다면 INVALID_IMAGE 였을 파일이다.
        assertCode(ImageExceptionCode.DIMENSIONS_TOO_LARGE) {
            validator.validate(ImageFixtures.pngHeaderOnly(6_000, 6_000), "image/png")
        }
    }

    @Test
    fun `빈 파일과 상한을 넘는 바이트는 거절한다`() {
        assertCode(ImageExceptionCode.EMPTY_FILE) { validator.validate(ByteArray(0), null) }
        assertCode(ImageExceptionCode.FILE_TOO_LARGE) { validator.validate(ByteArray(ImageValidator.MAX_BYTES + 1), null) }
    }

    private fun assertCode(
        code: ImageExceptionCode,
        block: () -> Unit,
    ) {
        assertThatThrownBy { block() }
            .isInstanceOfSatisfying(InvalidImageException::class.java) { assertThat(it.getCode()).isEqualTo(code) }
    }
}
