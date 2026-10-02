package core.application.image.application.service

import core.application.image.FakeImagePersistencePort
import core.application.image.FakeImageStoragePort
import core.application.image.ImageFixtures
import core.application.image.application.exception.ImageExceptionCode
import core.application.image.application.exception.ImageStorageUnavailableException
import core.application.image.application.exception.InvalidImageException
import core.application.image.application.validator.ImageValidator
import core.domain.image.enums.ImageContentType
import core.domain.member.vo.MemberId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ImageCommandServiceTest {
    private val owner = MemberId(7L)
    private val now = Instant.parse("2026-10-02T03:00:00Z")
    private val storage = FakeImageStoragePort()
    private val persistence = FakeImagePersistencePort()
    private val service = ImageCommandService(ImageValidator(), storage, persistence, Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun `업로드하면 객체를 올린 뒤 메타데이터를 저장한다`() {
        val bytes = ImageFixtures.png()

        val response = service.upload(owner, MockMultipartFile("file", "photo.jpg", "image/png", bytes))

        val saved = persistence.images.values.single()
        assertThat(response.imageId).isEqualTo(saved.id!!.value)
        assertThat(response.contentType).isEqualTo("image/png")
        assertThat(response.size).isEqualTo(bytes.size.toLong())
        assertThat(saved.ownerMemberId).isEqualTo(owner)
        assertThat(saved.contentType).isEqualTo(ImageContentType.PNG)
        assertThat(saved.createdAt).isEqualTo(now)
        assertThat(saved.objectKey).startsWith("images/").doesNotContain("photo")
        assertThat(storage.objects[saved.objectKey]).isEqualTo(bytes)
        assertThat(storage.calls).containsExactly("put")
    }

    @Test
    fun `빈 파일이나 잘못된 내용은 스토리지를 호출하지 않는다`() {
        assertThatThrownBy { service.upload(owner, MockMultipartFile("file", ByteArray(0))) }
            .isInstanceOf(InvalidImageException::class.java)
        assertThatThrownBy { service.upload(owner, MockMultipartFile("file", "a.png", "image/png", "not image".toByteArray())) }
            .isInstanceOf(InvalidImageException::class.java)

        assertThat(storage.calls).isEmpty()
        assertThat(persistence.images).isEmpty()
    }

    @Test
    fun `MultipartFile size 가 작게 보고돼도 상한까지만 읽고 거절한다`() {
        val oversized = ImageFixtures.png() + ByteArray(ImageValidator.MAX_BYTES)
        val lyingFile =
            object : MockMultipartFile("file", "a.png", "image/png", oversized) {
                override fun getSize(): Long = 100L
            }

        assertThatThrownBy { service.upload(owner, lyingFile) }
            .isInstanceOfSatisfying(InvalidImageException::class.java) {
                assertThat(it.getCode()).isEqualTo(ImageExceptionCode.FILE_TOO_LARGE)
            }
        assertThat(storage.calls).isEmpty()
    }

    @Test
    fun `업로드가 실패하면 메타데이터를 저장하지 않는다`() {
        storage.putFailure = ImageStorageUnavailableException()

        assertThatThrownBy { service.upload(owner, MockMultipartFile("file", "a.png", "image/png", ImageFixtures.png())) }
            .isSameAs(storage.putFailure)
        assertThat(persistence.images).isEmpty()
    }

    @Test
    fun `메타데이터 저장이 실패하면 올린 객체를 지우고 원래 예외를 던진다`() {
        val failure = IllegalStateException("commit failed")
        persistence.saveFailure = failure

        assertThatThrownBy { service.upload(owner, MockMultipartFile("file", "a.jpg", "image/jpeg", ImageFixtures.jpeg())) }
            .isSameAs(failure)
        assertThat(storage.calls).containsExactly("put", "delete")
        assertThat(storage.objects).isEmpty()
    }

    @Test
    fun `정리 삭제까지 실패해도 원래 예외를 던지고 삭제 실패는 suppressed 로 남긴다`() {
        val failure = IllegalStateException("commit failed")
        val cleanupFailure = ImageStorageUnavailableException()
        persistence.saveFailure = failure
        storage.deleteFailure = cleanupFailure

        assertThatThrownBy { service.upload(owner, MockMultipartFile("file", "a.png", null, ImageFixtures.png())) }
            .isSameAs(failure)
        assertThat(failure.suppressed).containsExactly(cleanupFailure)
        assertThat(storage.objects).hasSize(1)
    }
}
