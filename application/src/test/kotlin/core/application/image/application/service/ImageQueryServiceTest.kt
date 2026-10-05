package core.application.image.application.service

import core.application.image.FakeImagePersistencePort
import core.application.image.FakeImageStoragePort
import core.application.image.application.exception.ImageNotFoundException
import core.application.image.application.exception.ImageStorageUnavailableException
import core.application.image.application.properties.ImageStorageProperties
import core.domain.image.aggregate.Image
import core.domain.image.enums.ImageContentType
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ImageQueryServiceTest {
    private val owner = MemberId(7L)
    private val now = Instant.parse("2026-10-04T03:00:00Z")
    private val storage = FakeImageStoragePort()
    private val persistence = FakeImagePersistencePort()
    private val service = ImageQueryService(persistence, storage, ImageStorageProperties(), Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun `소유자는 1분짜리 읽기 URL 을 받는다`() {
        val image = store("images/existing-uuid")

        val response = service.getImage(owner, image.id!!)

        assertThat(response.expiresAt).isEqualTo(now.plusSeconds(60))
        assertThat(storage.pars.values).containsExactly("images/existing-uuid")
        assertThat(storage.calls).containsExactly("par-read")
        assertThat(response.url).contains("images/existing-uuid")
        assertThat(response.toString()).doesNotContain(response.url)
    }

    @Test
    fun `다운로드 URL 은 소유자에게만 원본 파일명으로 저장하는 1분짜리 읽기 URL 을 준다`() {
        val named = store("images/named", originalFileName = "진단서.png")
        val unnamed = store("images/unnamed")

        val response = service.getDownloadUrl(owner, named.id!!)
        service.getDownloadUrl(owner, unnamed.id!!)

        assertThat(response.expiresAt).isEqualTo(now.plusSeconds(60))
        assertThat(response.url).contains("images/named")
        assertThat(storage.downloadFileNames).containsExactly("진단서.png", null)
        storage.calls.clear()
        assertThatThrownBy { service.getDownloadUrl(MemberId(8L), named.id!!) }.isInstanceOf(ImageNotFoundException::class.java)
        assertThat(storage.calls).isEmpty()
    }

    @Test
    fun `직접 업로드 전에 저장된 이미지도 같은 방식으로 읽힌다`() {
        // 이전 multipart 업로드는 images/{임의 UUID} 키였다.
        val legacy = store("images/0f8c5a3e-2b7d-4d8e-9a51-7c6e2f1b3a90")

        assertThat(service.getImage(owner, legacy.id!!).url).contains(legacy.objectKey)
    }

    @Test
    fun `남의 이미지와 없는 이미지는 스토리지를 부르지 않고 404`() {
        val image = store("images/a")

        assertThatThrownBy { service.getImage(MemberId(8L), image.id!!) }.isInstanceOf(ImageNotFoundException::class.java)
        assertThatThrownBy { service.getImage(owner, ImageId(999L)) }.isInstanceOf(ImageNotFoundException::class.java)
        assertThat(storage.calls).isEmpty()
    }

    @Test
    fun `읽기 URL 을 만들지 못하면 503`() {
        val image = store("images/a")
        storage.createParFailure = ImageStorageUnavailableException()

        assertThatThrownBy { service.getImage(owner, image.id!!) }.isInstanceOf(ImageStorageUnavailableException::class.java)
    }

    private fun store(
        objectKey: String,
        originalFileName: String? = null,
    ): Image {
        val image = Image(ImageId(persistence.images.size + 1L), owner, objectKey, ImageContentType.PNG, 10L, now, originalFileName)
        persistence.images[image.id!!] = image
        return image
    }
}
