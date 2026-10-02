package core.application.image.application.service

import core.application.image.FakeImagePersistencePort
import core.application.image.FakeImageStoragePort
import core.application.image.ImageFixtures
import core.application.image.application.exception.ImageNotFoundException
import core.application.image.application.exception.ImageStorageUnavailableException
import core.domain.image.aggregate.Image
import core.domain.image.enums.ImageContentType
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class ImageQueryServiceTest {
    private val owner = MemberId(7L)
    private val storage = FakeImageStoragePort()
    private val persistence = FakeImagePersistencePort()
    private val service = ImageQueryService(persistence, storage)

    @Test
    fun `소유자는 원본 바이트를 받는다`() {
        val bytes = ImageFixtures.jpeg()
        val image = store(bytes)

        val content = service.getImage(owner, image.id!!)

        assertThat(content.bytes).isEqualTo(bytes)
        assertThat(content.contentType).isEqualTo(ImageContentType.JPEG)
    }

    @Test
    fun `남의 이미지는 없는 이미지와 같은 404 이며 스토리지를 호출하지 않는다`() {
        val image = store(ImageFixtures.png())

        assertThatThrownBy { service.getImage(MemberId(8L), image.id!!) }.isInstanceOf(ImageNotFoundException::class.java)
        assertThatThrownBy { service.getImage(owner, ImageId(999L)) }.isInstanceOf(ImageNotFoundException::class.java)
        assertThat(storage.calls).isEmpty()
    }

    @Test
    fun `저장된 바이트 크기가 메타데이터와 다르면 503 으로 처리한다`() {
        val image = store(ImageFixtures.png())
        storage.objects[image.objectKey] = ByteArray(3)

        assertThatThrownBy { service.getImage(owner, image.id!!) }.isInstanceOf(ImageStorageUnavailableException::class.java)
    }

    private fun store(bytes: ByteArray): Image {
        val type = if (bytes[0] == 0xFF.toByte()) ImageContentType.JPEG else ImageContentType.PNG
        val image = persistence.save(Image.create(owner, type, bytes.size.toLong(), Instant.EPOCH))
        storage.objects[image.objectKey] = bytes
        return image
    }
}
