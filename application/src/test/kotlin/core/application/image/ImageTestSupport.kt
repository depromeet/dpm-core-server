package core.application.image

import core.application.image.application.exception.ImageNotFoundException
import core.domain.image.aggregate.Image
import core.domain.image.port.outbound.ImagePersistencePort
import core.domain.image.port.outbound.ImageStoragePort
import core.domain.image.vo.ImageId
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import javax.imageio.ImageIO

object ImageFixtures {
    fun png(
        width: Int = 4,
        height: Int = 3,
    ): ByteArray = encode("png", width, height)

    fun jpeg(
        width: Int = 4,
        height: Int = 3,
    ): ByteArray = encode("jpeg", width, height)

    fun gif(): ByteArray = encode("gif", 2, 2)

    /** IHDR 만 있고 픽셀 데이터는 없는 PNG. 헤더 크기만으로 거절되는지(디코드 전) 확인할 때 쓴다. */
    fun pngHeaderOnly(
        width: Int,
        height: Int,
    ): ByteArray {
        val ihdr = ByteBuffer.allocate(13).putInt(width).putInt(height).put(byteArrayOf(8, 2, 0, 0, 0)).array()
        return ByteArrayOutputStream()
            .apply {
                write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
                write(chunk("IHDR", ihdr))
                write(chunk("IEND", ByteArray(0)))
            }.toByteArray()
    }

    private fun chunk(
        type: String,
        data: ByteArray,
    ): ByteArray {
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        val crc = CRC32().apply { update(typeBytes + data) }.value.toInt()
        return ByteBuffer.allocate(12 + data.size).putInt(data.size).put(typeBytes).put(data).putInt(crc).array()
    }

    private fun encode(
        format: String,
        width: Int,
        height: Int,
    ): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        image.setRGB(0, 0, 0xFF0000)
        return ByteArrayOutputStream().also { check(ImageIO.write(image, format, it)) }.toByteArray()
    }
}

class FakeImagePersistencePort : ImagePersistencePort {
    val images = mutableMapOf<ImageId, Image>()
    var saveFailure: Exception? = null
    private var sequence = 0L

    override fun save(image: Image): Image {
        saveFailure?.let { throw it }
        val saved =
            Image(
                id = image.id ?: ImageId(++sequence),
                ownerMemberId = image.ownerMemberId,
                objectKey = image.objectKey,
                contentType = image.contentType,
                size = image.size,
                createdAt = image.createdAt,
            )
        images[saved.id!!] = saved
        return saved
    }

    override fun findById(imageId: ImageId): Image? = images[imageId]
}

class FakeImageStoragePort : ImageStoragePort {
    val objects = mutableMapOf<String, ByteArray>()
    val calls = mutableListOf<String>()
    var putFailure: Exception? = null
    var deleteFailure: Exception? = null

    override fun put(
        objectKey: String,
        content: ByteArray,
        contentType: String,
    ) {
        calls += "put"
        putFailure?.let { throw it }
        objects[objectKey] = content
    }

    override fun get(
        objectKey: String,
        maxBytes: Long,
    ): ByteArray {
        calls += "get"
        return objects[objectKey] ?: throw ImageNotFoundException()
    }

    override fun delete(objectKey: String) {
        calls += "delete"
        deleteFailure?.let { throw it }
        objects.remove(objectKey)
    }
}
