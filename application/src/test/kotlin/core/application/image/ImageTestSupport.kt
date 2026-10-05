package core.application.image

import core.application.image.application.exception.ImageStorageUnavailableException
import core.domain.image.aggregate.Image
import core.domain.image.aggregate.ImageUpload
import core.domain.image.enums.ImageUploadStatus
import core.domain.image.port.outbound.CopyStart
import core.domain.image.port.outbound.CopyStatus
import core.domain.image.port.outbound.DownloadedObject
import core.domain.image.port.outbound.ImagePersistencePort
import core.domain.image.port.outbound.ImageStoragePort
import core.domain.image.port.outbound.ImageUploadPersistencePort
import core.domain.image.port.outbound.PreauthenticatedUrl
import core.domain.image.port.outbound.StoredObject
import core.domain.image.vo.ImageId
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
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

    override fun findById(imageId: ImageId): Image? = images[imageId]

    override fun findAllByIds(imageIds: List<ImageId>): List<Image> = imageIds.mapNotNull { images[it] }
}

/** 실제 저장소의 조건부 UPDATE 와 같은 조건으로 동작하는 메모리 구현. 이미지 행은 [images] 에 넣는다. */
class FakeImageUploadPersistencePort(
    private val images: FakeImagePersistencePort = FakeImagePersistencePort(),
) : ImageUploadPersistencePort {
    val uploads = mutableMapOf<String, ImageUpload>()
    var completeFailure: Exception? = null
    private var imageSequence = 0L

    override fun save(upload: ImageUpload): ImageUpload = upload.also { uploads[it.id] = it }

    override fun findById(uploadId: String): ImageUpload? = uploads[uploadId]

    override fun startVerification(
        uploadId: String,
        token: String,
        now: Instant,
        leaseUntil: Instant,
    ): Boolean =
        update(uploadId, {
            it.expiresAt.isAfter(now) &&
                (
                    it.status == ImageUploadStatus.PENDING ||
                        (it.status == ImageUploadStatus.VERIFYING && it.isLeaseFree(now))
                )
        }) { it.copyWith(status = ImageUploadStatus.VERIFYING, leaseToken = token, leaseUntil = leaseUntil) }

    override fun releaseVerification(
        uploadId: String,
        token: String,
    ): Boolean =
        update(uploadId, { it.status == ImageUploadStatus.VERIFYING && it.leaseToken == token }) {
            it.copyWith(status = ImageUploadStatus.PENDING, leaseToken = null, leaseUntil = null)
        }

    override fun markValidated(
        uploadId: String,
        token: String,
        etag: String,
        leaseUntil: Instant,
    ): Boolean =
        update(uploadId, { it.status == ImageUploadStatus.VERIFYING && it.leaseToken == token }) {
            it.copyWith(status = ImageUploadStatus.COPYING, etag = etag, leaseUntil = leaseUntil)
        }

    override fun reject(
        uploadId: String,
        token: String,
        failureCode: String,
    ): Boolean =
        update(uploadId, { it.status == ImageUploadStatus.VERIFYING && it.leaseToken == token }) {
            it.copyWith(
                status = ImageUploadStatus.REJECTED,
                failureCode = failureCode,
                leaseToken = null,
                leaseUntil = null,
            )
        }

    override fun acquireCopy(
        uploadId: String,
        token: String,
        now: Instant,
        leaseUntil: Instant,
    ): Boolean =
        update(uploadId, { it.status == ImageUploadStatus.COPYING && it.isLeaseFree(now) }) {
            it.copyWith(leaseToken = token, leaseUntil = leaseUntil)
        }

    override fun recordCopyWorkRequest(
        uploadId: String,
        token: String,
        workRequestId: String,
    ): Boolean =
        update(uploadId, { it.status == ImageUploadStatus.COPYING && it.leaseToken == token }) {
            it.copyWith(workRequestId = workRequestId)
        }

    override fun releaseCopy(
        uploadId: String,
        token: String,
    ): Boolean =
        update(uploadId, { it.status == ImageUploadStatus.COPYING && it.leaseToken == token }) {
            it.copyWith(leaseToken = null, leaseUntil = null)
        }

    override fun fail(
        uploadId: String,
        token: String,
        failureCode: String,
    ): Boolean =
        update(uploadId, { it.status == ImageUploadStatus.COPYING && it.leaseToken == token }) {
            it.copyWith(
                status = ImageUploadStatus.FAILED,
                failureCode = failureCode,
                leaseToken = null,
                leaseUntil = null,
            )
        }

    override fun complete(
        uploadId: String,
        token: String,
        image: Image,
    ): Image? {
        completeFailure?.let { throw it }
        val upload = uploads[uploadId] ?: return null
        if (upload.status != ImageUploadStatus.COPYING || upload.leaseToken != token) return null
        check(images.images.values.none { it.objectKey == image.objectKey }) { "uk_images_object_key 위반" }
        val id = ImageId(++imageSequence)
        val saved = Image(id, image.ownerMemberId, image.objectKey, image.contentType, image.size, image.createdAt)
        images.images[saved.id!!] = saved
        uploads[uploadId] =
            upload.copyWith(status = ImageUploadStatus.COMPLETED, imageId = id, leaseToken = null, leaseUntil = null)
        return saved
    }

    override fun clearParId(uploadId: String): Boolean =
        update(uploadId, { it.status == ImageUploadStatus.COMPLETED }) { it.copyWith(parId = null) }

    override fun expire(
        uploadId: String,
        now: Instant,
    ): Boolean =
        update(uploadId, {
            (it.status == ImageUploadStatus.PENDING || it.status == ImageUploadStatus.VERIFYING) && it.isLeaseFree(now)
        }) { it.copyWith(status = ImageUploadStatus.EXPIRED, leaseToken = null, leaseUntil = null) }

    override fun deleteTerminal(uploadId: String): Boolean {
        val upload = uploads[uploadId] ?: return false
        val terminal = setOf(ImageUploadStatus.REJECTED, ImageUploadStatus.FAILED, ImageUploadStatus.EXPIRED)
        if (upload.status !in terminal) return false
        uploads.remove(uploadId)
        return true
    }

    override fun findStale(
        createdBefore: Instant,
        limit: Int,
    ): List<ImageUpload> =
        uploads.values
            .filter { it.parId != null && it.createdAt.isBefore(createdBefore) }
            .sortedBy { it.createdAt }
            .take(limit)

    private fun update(
        uploadId: String,
        condition: (ImageUpload) -> Boolean,
        change: (ImageUpload) -> ImageUpload,
    ): Boolean {
        val upload = uploads[uploadId]?.takeIf(condition) ?: return false
        uploads[uploadId] = change(upload)
        return true
    }
}

/** 테스트에서 세션 일부만 바꿀 때 쓴다. null 로 비우려면 명시적으로 넘긴다. */
fun ImageUpload.copyWith(
    status: ImageUploadStatus = this.status,
    parId: String? = this.parId,
    leaseToken: String? = this.leaseToken,
    leaseUntil: Instant? = this.leaseUntil,
    etag: String? = this.etag,
    workRequestId: String? = this.workRequestId,
    imageId: ImageId? = this.imageId,
    failureCode: String? = this.failureCode,
    expiresAt: Instant = this.expiresAt,
    createdAt: Instant = this.createdAt,
): ImageUpload =
    ImageUpload(
        id = id,
        ownerMemberId = ownerMemberId,
        contentType = contentType,
        size = size,
        parId = parId,
        status = status,
        expiresAt = expiresAt,
        leaseToken = leaseToken,
        leaseUntil = leaseUntil,
        etag = etag,
        workRequestId = workRequestId,
        imageId = imageId,
        failureCode = failureCode,
        createdAt = createdAt,
    )

/**
 * 버킷 흉내. 객체마다 ETag 를 붙이고, 복사는 원본 ETag 조건과 대상 if-none-match 를 실제처럼 따진다.
 * [copyCompletesImmediately] 가 false 면 복사는 [finishCopies] 전까지 진행 중이다.
 */
class FakeImageStoragePort : ImageStoragePort {
    class StoredBytes(
        val bytes: ByteArray,
        val etag: String,
        val contentType: String?,
        val contentEncoding: String? = null,
    )

    val objects = mutableMapOf<String, StoredBytes>()
    val pars = mutableMapOf<String, String>()
    val revokedPars = mutableListOf<String>()
    val calls = mutableListOf<String>()
    val downloadTargets = mutableListOf<Path>()
    val workRequests = mutableMapOf<String, CopyStatus>()
    private val pendingCopies = mutableMapOf<String, Pair<String, String>>()

    var copyCompletesImmediately = true
    var createParFailure: Exception? = null
    var downloadFailure: Exception? = null
    var deleteFailure: Exception? = null
    var revokeFailure: Exception? = null

    /** 복사 상태 조회 실패(권한 없음 404 등은 어댑터가 503 으로 바꾼다). */
    var copyStatusFailure: Exception? = null

    /** 복사 상태를 읽기 직전에 실행한다. HEAD 와 상태 조회 사이에 복사가 끝나는 경쟁을 흉내 낼 때 쓴다. */
    var beforeCopyStatus: () -> Unit = {}

    /** 복사를 실제로 시작한 뒤 응답을 잃은 것처럼 던진다(work request id 를 모름). */
    var copyResponseLost = false

    /** 원본을 실제로 복사하지 않고 응답 없이 실패한다. */
    var copyStartFailure: Exception? = null

    /** 내려받기 직전에 실행한다. 동시성 테스트에서 검증을 붙잡아 둘 때 쓴다. */
    var beforeDownload: () -> Unit = {}

    /** 내려받은 직후에 실행한다. 검증 뒤 원본이 바뀌는 경쟁을 흉내 낼 때 쓴다. */
    var afterDownload: () -> Unit = {}

    /** 쓰기 PAR 을 만든 객체 키(회수 여부와 무관). */
    val writeParKeys = mutableListOf<String>()

    /** 낸 복사 요청 (원본, 원본 ETag 조건, 대상). */
    val copyRequests = mutableListOf<Triple<String, String, String>>()
    private var sequence = 0

    /** 프론트가 업로드 URL 로 PUT 한 것과 같다. 매번 새 ETag 가 생긴다. */
    fun put(
        objectKey: String,
        bytes: ByteArray,
        contentType: String? = "image/png",
        contentEncoding: String? = null,
    ) {
        objects[objectKey] = StoredBytes(bytes, "etag-${++sequence}", contentType, contentEncoding)
    }

    fun finishCopies() {
        pendingCopies.forEach { (workRequestId, keys) ->
            val (source, destination) = keys
            objects[source]?.let { objects[destination] = StoredBytes(it.bytes, "etag-${++sequence}", it.contentType) }
            workRequests[workRequestId] = CopyStatus.COMPLETED
        }
        pendingCopies.clear()
    }

    /** 진행 중인 복사를 대상에 쓰지 않고 실패로 끝낸다. */
    fun failCopies() {
        pendingCopies.keys.forEach { workRequests[it] = CopyStatus.FAILED }
        pendingCopies.clear()
    }

    override fun createUploadUrl(
        objectKey: String,
        expiresAt: Instant,
    ): PreauthenticatedUrl {
        writeParKeys += objectKey
        return createPar("write", objectKey, expiresAt)
    }

    override fun createReadUrl(
        objectKey: String,
        expiresAt: Instant,
    ): PreauthenticatedUrl = createPar("read", objectKey, expiresAt)

    override fun revokeUrl(parId: String) {
        calls += "revoke"
        revokeFailure?.let { throw it }
        pars.remove(parId)
        revokedPars += parId
    }

    override fun download(
        objectKey: String,
        maxBytes: Long,
        target: Path,
    ): DownloadedObject? {
        calls += "download"
        downloadTargets.add(target)
        beforeDownload()
        downloadFailure?.let {
            Files.write(target, byteArrayOf(1, 2, 3))
            throw it
        }
        val stored = objects[objectKey] ?: return null
        val written = stored.bytes.copyOf(minOf(stored.bytes.size.toLong(), maxBytes + 1).toInt())
        Files.write(target, written)
        afterDownload()
        return DownloadedObject(stored.etag, written.size.toLong(), stored.contentType, stored.contentEncoding)
    }

    override fun head(objectKey: String): StoredObject? {
        calls += "head"
        return objects[objectKey]?.let { StoredObject(it.etag, it.bytes.size.toLong()) }
    }

    override fun startCopy(
        sourceKey: String,
        sourceEtag: String,
        destinationKey: String,
    ): CopyStart {
        calls += "copy"
        copyRequests += Triple(sourceKey, sourceEtag, destinationKey)
        copyStartFailure?.let { throw it }
        val source = objects[sourceKey]
        if (source == null || source.etag != sourceEtag || objects.containsKey(destinationKey)) {
            return CopyStart.PreconditionFailed
        }
        val workRequestId = "wr-${++sequence}"
        pendingCopies[workRequestId] = sourceKey to destinationKey
        workRequests[workRequestId] = CopyStatus.IN_PROGRESS
        if (copyCompletesImmediately) finishCopies()
        if (copyResponseLost) throw ImageStorageUnavailableException()
        return CopyStart.Started(workRequestId)
    }

    override fun copyStatus(workRequestId: String): CopyStatus {
        calls += "copy-status"
        beforeCopyStatus()
        copyStatusFailure?.let { throw it }
        return workRequests[workRequestId] ?: CopyStatus.FAILED
    }

    override fun delete(objectKey: String) {
        calls += "delete"
        deleteFailure?.let { throw it }
        objects.remove(objectKey)
    }

    private fun createPar(
        access: String,
        objectKey: String,
        expiresAt: Instant,
    ): PreauthenticatedUrl {
        calls += "par-$access"
        createParFailure?.let { throw it }
        val parId = "par-${++sequence}"
        pars[parId] = objectKey
        return PreauthenticatedUrl(parId, "https://objectstorage.test/p/secret-$parId/o/$objectKey", expiresAt)
    }
}
