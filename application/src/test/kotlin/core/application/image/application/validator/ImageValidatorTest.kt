package core.application.image.application.validator

import core.application.image.ImageFixtures
import core.application.image.application.exception.ImageExceptionCode
import core.application.image.application.exception.InvalidImageException
import core.domain.image.enums.ImageContentType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ImageValidatorTest {
    private val validator = ImageValidator()

    @TempDir
    lateinit var tempDir: Path

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

    @Test
    fun `요청 Content-Type 은 파라미터와 대소문자를 무시하고 JPEG PNG 만 받는다`() {
        assertThat(validator.parseContentType("IMAGE/PNG; charset=binary")).isEqualTo(ImageContentType.PNG)
        assertThat(validator.parseContentType("image/jpeg")).isEqualTo(ImageContentType.JPEG)
        assertThat(validator.parseContentType("image/gif")).isNull()
        assertThat(validator.parseContentType("not a type")).isNull()
        assertThat(validator.parseContentType(null)).isNull()
    }

    @Test
    fun `원본 파일명은 경로와 제어 문자를 떼고, 비어 있으면 null, 255자를 넘으면 거절한다`() {
        assertThat(validator.normalizeFileName("  진단서.jpg ")).isEqualTo("진단서.jpg")
        assertThat(validator.normalizeFileName("C:\\fakepath\\진단서.jpg")).isEqualTo("진단서.jpg")
        assertThat(validator.normalizeFileName("../../etc/passwd")).isEqualTo("passwd")
        assertThat(validator.normalizeFileName("a\u0000b\nc.png")).isEqualTo("abc.png")
        assertThat(validator.normalizeFileName("   ")).isNull()
        assertThat(validator.normalizeFileName("dir/")).isNull()
        assertThat(validator.normalizeFileName(null)).isNull()
        assertThat(validator.normalizeFileName("a".repeat(ImageValidator.MAX_FILE_NAME_LENGTH))).hasSize(255)
        assertCode(ImageExceptionCode.FILE_NAME_TOO_LONG) {
            validator.normalizeFileName("a".repeat(ImageValidator.MAX_FILE_NAME_LENGTH + 1))
        }
    }

    @Test
    fun `파일명 길이는 UTF-16 단위가 아니라 문자 수로 센다`() {
        val emoji = "\uD83D\uDCF7" // 📷, 한 글자지만 String.length 는 2
        val maxEmojiName = emoji.repeat(ImageValidator.MAX_FILE_NAME_LENGTH)

        assertThat(validator.normalizeFileName(maxEmojiName)).isEqualTo(maxEmojiName)
        assertCode(ImageExceptionCode.FILE_NAME_TOO_LONG) { validator.normalizeFileName(maxEmojiName + "a") }
    }

    /** 검증은 파일만 받는다. 바이트를 임시 파일로 써서 넘긴다. */
    private fun ImageValidator.validate(
        bytes: ByteArray,
        declaredContentType: String?,
    ): ImageContentType = validate(Files.write(Files.createTempFile(tempDir, "image-", ".bin"), bytes), declaredContentType)

    private fun assertCode(
        code: ImageExceptionCode,
        block: () -> Unit,
    ) {
        assertThatThrownBy { block() }
            .isInstanceOfSatisfying(InvalidImageException::class.java) { assertThat(it.getCode()).isEqualTo(code) }
    }
}
