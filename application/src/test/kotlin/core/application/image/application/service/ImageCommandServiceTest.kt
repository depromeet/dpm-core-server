package core.application.image.application.service

import core.application.image.FakeImagePersistencePort
import core.application.image.FakeImageStoragePort
import core.application.image.FakeImageUploadPersistencePort
import core.application.image.ImageFixtures
import core.application.image.application.dto.ImageUploadCompletion
import core.application.image.application.exception.ImageExceptionCode
import core.application.image.application.exception.ImageStorageUnavailableException
import core.application.image.application.exception.ImageUploadException
import core.application.image.application.exception.ImageVerificationBusyException
import core.application.image.application.exception.InvalidImageException
import core.application.image.application.properties.ImageStorageProperties
import core.application.image.application.validator.ImageValidator
import core.application.image.copyWith
import core.application.support.MutableClock
import core.domain.image.aggregate.ImageUpload
import core.domain.image.enums.ImageContentType
import core.domain.image.enums.ImageUploadStatus
import core.domain.image.port.outbound.CopyStatus
import core.domain.member.vo.MemberId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ImageCommandServiceTest {
    private val owner = MemberId(7L)
    private val clock = MutableClock(Instant.parse("2026-10-04T03:00:00Z"))
    private val storage = FakeImageStoragePort()
    private val images = FakeImagePersistencePort()
    private val uploads = FakeImageUploadPersistencePort(images)
    private val properties = ImageStorageProperties()
    private val service = ImageCommandService(ImageValidator(), storage, uploads, properties, clock)

    @Test
    fun `업로드 URL 은 업로드 키 하나에만 쓰기 PAR 을 걸고 세션을 PENDING 으로 남긴다`() {
        val response = service.createUpload(owner, "image/png; charset=binary", 123L)

        val upload = uploads.uploads.getValue(response.uploadId)
        assertThat(upload.status).isEqualTo(ImageUploadStatus.PENDING)
        assertThat(upload.ownerMemberId).isEqualTo(owner)
        assertThat(upload.contentType).isEqualTo(ImageContentType.PNG)
        assertThat(upload.size).isEqualTo(123L)
        assertThat(upload.expiresAt).isEqualTo(clock.now.plus(Duration.ofMinutes(10)))
        assertThat(storage.pars[upload.parId]).isEqualTo("uploads/${response.uploadId}")
        assertThat(response.uploadUrl).contains("secret")
        assertThat(response.toString()).doesNotContain("secret")
        assertThat(upload.toString()).doesNotContain(upload.parId!!)
    }

    @Test
    fun `지원하지 않는 형식과 잘못된 크기는 PAR 을 만들기 전에 거절한다`() {
        assertCode(ImageExceptionCode.UNSUPPORTED_TYPE) { service.createUpload(owner, "image/gif", 10L) }
        assertCode(ImageExceptionCode.EMPTY_FILE) { service.createUpload(owner, "image/png", 0L) }
        assertCode(ImageExceptionCode.FILE_TOO_LARGE) {
            service.createUpload(owner, "image/png", ImageValidator.MAX_BYTES + 1L)
        }
        assertThat(storage.calls).isEmpty()
    }

    @Test
    fun `완료하면 검증한 바이트를 확정 키로 복사한 뒤에만 이미지 행을 만든다`() {
        val bytes = ImageFixtures.png()
        val upload = uploaded(bytes)

        val result = service.completeUpload(owner, upload.id) as ImageUploadCompletion.Completed

        val image = images.images.values.single()
        assertThat(result.image.imageId).isEqualTo(image.id!!.value)
        assertThat(result.image.contentType).isEqualTo("image/png")
        assertThat(result.image.size).isEqualTo(bytes.size.toLong())
        assertThat(image.objectKey).isEqualTo("images/${upload.id}")
        assertThat(storage.objects["images/${upload.id}"]!!.bytes).isEqualTo(bytes)
        // 업로드 객체·쓰기 PAR 은 지우고 정리 완료(parId = null)를 남긴다.
        assertThat(storage.objects).doesNotContainKey(upload.stagingKey)
        assertThat(storage.revokedPars).contains(upload.parId)
        val completed = uploads.uploads.getValue(upload.id)
        assertThat(completed.status).isEqualTo(ImageUploadStatus.COMPLETED)
        assertThat(completed.imageId).isEqualTo(image.id)
        assertThat(completed.parId).isNull()
        assertThat(completed.leaseToken).isNull()
        assertTempFilesDeleted()
    }

    @Test
    fun `확정 키에는 쓰기 PAR 이 없고 모든 복사는 검증한 ETag 를 원본 조건으로 건다`() {
        storage.copyCompletesImmediately = false
        storage.copyStartFailure = ImageStorageUnavailableException()
        val upload = uploaded(ImageFixtures.png())

        assertThatThrownBy { service.completeUpload(owner, upload.id) }.isInstanceOf(ImageStorageUnavailableException::class.java)
        storage.copyStartFailure = null
        service.completeUpload(owner, upload.id)
        storage.finishCopies()
        service.completeUpload(owner, upload.id)

        val validated = uploads.uploads.getValue(upload.id)
        assertThat(storage.writeParKeys).allSatisfy { assertThat(it).startsWith("uploads/") }
        assertThat(storage.writeParKeys).doesNotContain(validated.finalKey)
        assertThat(storage.copyRequests).hasSize(2)
        assertThat(storage.copyRequests).allSatisfy {
            assertThat(it).isEqualTo(Triple(validated.stagingKey, validated.etag, validated.finalKey))
        }
    }

    @Test
    fun `완료를 다시 불러도 같은 imageId 를 주고 저장소를 다시 부르지 않는다`() {
        val upload = uploaded(ImageFixtures.png())
        val first = service.completeUpload(owner, upload.id) as ImageUploadCompletion.Completed
        storage.calls.clear()

        val second = service.completeUpload(owner, upload.id) as ImageUploadCompletion.Completed

        assertThat(second.image).isEqualTo(first.image)
        assertThat(images.images).hasSize(1)
        assertThat(storage.calls).isEmpty()
    }

    @Test
    fun `남의 업로드와 형식이 틀린 id 는 저장소를 부르지 않고 404`() {
        val upload = uploaded(ImageFixtures.png())
        storage.calls.clear()

        assertUploadCode(ImageExceptionCode.UPLOAD_NOT_FOUND) { service.completeUpload(MemberId(8L), upload.id) }
        assertUploadCode(ImageExceptionCode.UPLOAD_NOT_FOUND) { service.completeUpload(owner, "not-a-uuid") }
        assertThat(storage.calls).isEmpty()
        assertThat(uploads.uploads.getValue(upload.id).status).isEqualTo(ImageUploadStatus.PENDING)
    }

    @Test
    fun `아직 PUT 하지 않았으면 409 를 주고 다시 PENDING 으로 둔다`() {
        val created = service.createUpload(owner, "image/png", 10L)

        assertUploadCode(ImageExceptionCode.NOT_UPLOADED) { service.completeUpload(owner, created.uploadId) }

        val upload = uploads.uploads.getValue(created.uploadId)
        assertThat(upload.status).isEqualTo(ImageUploadStatus.PENDING)
        assertThat(upload.leaseToken).isNull()
        assertTempFilesDeleted()
    }

    @Test
    fun `손상된 이미지는 거절하고 업로드를 정리하며 같은 오류를 반복해서 준다`() {
        val png = ImageFixtures.png(64, 64)
        val upload = uploaded(png.copyOf(png.size / 2))

        assertCode(ImageExceptionCode.INVALID_IMAGE) { service.completeUpload(owner, upload.id) }

        val rejected = uploads.uploads.getValue(upload.id)
        assertThat(rejected.status).isEqualTo(ImageUploadStatus.REJECTED)
        assertThat(rejected.failureCode).isEqualTo("INVALID_IMAGE")
        assertThat(storage.objects).doesNotContainKey(upload.stagingKey)
        assertThat(storage.revokedPars).contains(upload.parId)
        assertThat(images.images).isEmpty()

        storage.calls.clear()
        assertThatThrownBy { service.completeUpload(owner, upload.id) }
            .isInstanceOfSatisfying(ImageUploadException::class.java) {
                assertThat(it.getCode()).isEqualTo(ImageExceptionCode.INVALID_IMAGE)
            }
        assertThat(storage.calls).isEmpty()
        assertTempFilesDeleted()
    }

    @Test
    fun `상한을 넘는 업로드는 상한 + 1 바이트까지만 받고 거절한다`() {
        val upload = uploaded(ByteArray(ImageValidator.MAX_BYTES + 5), declaredSize = ImageValidator.MAX_BYTES.toLong())
        val readSizes = mutableListOf<Long>()
        storage.afterDownload = { readSizes += Files.size(storage.downloadTargets.last()) }

        assertCode(ImageExceptionCode.FILE_TOO_LARGE) { service.completeUpload(owner, upload.id) }

        assertThat(readSizes.single()).isEqualTo(ImageValidator.MAX_BYTES + 1L)
        assertThat(storage.objects).doesNotContainKey(upload.stagingKey)
        assertTempFilesDeleted()
    }

    @Test
    fun `실제 크기나 PUT 헤더가 발급 요청과 다르면 거절한다`() {
        val bytes = ImageFixtures.png()
        val sizeMismatch = uploaded(bytes, declaredSize = bytes.size + 1L)
        assertCode(ImageExceptionCode.SIZE_MISMATCH) { service.completeUpload(owner, sizeMismatch.id) }

        val octetStream = uploaded(bytes, putContentType = "application/octet-stream")
        assertCode(ImageExceptionCode.CONTENT_TYPE_MISMATCH) { service.completeUpload(owner, octetStream.id) }

        val encoded = uploaded(bytes, putContentEncoding = "gzip")
        assertCode(ImageExceptionCode.CONTENT_TYPE_MISMATCH) { service.completeUpload(owner, encoded.id) }

        val jpegAsPng = uploaded(ImageFixtures.jpeg())
        assertCode(ImageExceptionCode.CONTENT_TYPE_MISMATCH) { service.completeUpload(owner, jpegAsPng.id) }
        assertThat(images.images).isEmpty()
    }

    @Test
    fun `업로드 URL 이 만료되면 새 검증을 시작하지 않는다`() {
        val upload = uploaded(ImageFixtures.png())
        clock.now = upload.expiresAt
        storage.calls.clear()

        assertUploadCode(ImageExceptionCode.UPLOAD_EXPIRED) { service.completeUpload(owner, upload.id) }
        assertThat(storage.calls).isEmpty()
    }

    @Test
    fun `복사가 끝나지 않았으면 202 이고, 끝난 뒤 같은 호출이 이미지를 만든다`() {
        storage.copyCompletesImmediately = false
        val upload = uploaded(ImageFixtures.png())

        assertThat(service.completeUpload(owner, upload.id)).isEqualTo(ImageUploadCompletion.InProgress)
        val copying = uploads.uploads.getValue(upload.id)
        assertThat(copying.status).isEqualTo(ImageUploadStatus.COPYING)
        assertThat(copying.workRequestId).isNotNull()
        assertThat(copying.leaseToken).isNull()
        assertThat(images.images).isEmpty()

        // URL 만료 뒤에도 이미 복사 중인 업로드는 끝까지 진행한다.
        clock.now = upload.expiresAt.plusSeconds(60)
        assertThat(service.completeUpload(owner, upload.id)).isEqualTo(ImageUploadCompletion.InProgress)
        storage.finishCopies()

        assertThat(service.completeUpload(owner, upload.id)).isInstanceOf(ImageUploadCompletion.Completed::class.java)
        assertThat(images.images).hasSize(1)
        assertThat(storage.copyRequests).hasSize(1)
    }

    @Test
    fun `검증 뒤 원본이 바뀌면 복사가 거절되고 이미지를 만들지 않는다`() {
        val upload = uploaded(ImageFixtures.png())
        storage.afterDownload = { storage.put(upload.stagingKey, ImageFixtures.png(8, 8)) }

        assertUploadCode(ImageExceptionCode.SOURCE_CHANGED) { service.completeUpload(owner, upload.id) }

        assertThat(uploads.uploads.getValue(upload.id).status).isEqualTo(ImageUploadStatus.FAILED)
        assertThat(storage.objects).doesNotContainKey(upload.finalKey)
        assertThat(images.images).isEmpty()
        assertUploadCode(ImageExceptionCode.SOURCE_CHANGED) { service.completeUpload(owner, upload.id) }
    }

    @Test
    fun `복사 응답을 잃었는데 복사가 끝났으면 다시 복사하지 않고 확정한다`() {
        storage.copyResponseLost = true
        val upload = uploaded(ImageFixtures.png())

        assertThatThrownBy { service.completeUpload(owner, upload.id) }.isInstanceOf(ImageStorageUnavailableException::class.java)
        val lost = uploads.uploads.getValue(upload.id)
        assertThat(lost.status).isEqualTo(ImageUploadStatus.COPYING)
        assertThat(lost.workRequestId).isNull()
        assertThat(lost.leaseToken).isNull()
        assertThat(images.images).isEmpty()

        storage.copyResponseLost = false
        assertThat(service.completeUpload(owner, upload.id)).isInstanceOf(ImageUploadCompletion.Completed::class.java)
        assertThat(storage.copyRequests).hasSize(1)
        assertThat(images.images).hasSize(1)
    }

    @Test
    fun `복사가 시작되지 않은 채 실패했으면 같은 조건으로 다시 복사한다`() {
        storage.copyStartFailure = ImageStorageUnavailableException()
        val upload = uploaded(ImageFixtures.png())
        assertThatThrownBy { service.completeUpload(owner, upload.id) }.isInstanceOf(ImageStorageUnavailableException::class.java)
        // 503 은 재시도 대상이라 업로드 객체와 COPYING 을 그대로 둔다.
        assertThat(uploads.uploads.getValue(upload.id).status).isEqualTo(ImageUploadStatus.COPYING)
        assertThat(storage.objects).containsKey(upload.stagingKey)

        storage.copyStartFailure = null
        assertThat(service.completeUpload(owner, upload.id)).isInstanceOf(ImageUploadCompletion.Completed::class.java)
        assertThat(storage.copyRequests).hasSize(2)
        assertThat(images.images).hasSize(1)
    }

    @Test
    fun `확정 객체가 있으면 복사 상태를 읽지 못해도 확정한다`() {
        storage.copyCompletesImmediately = false
        val upload = uploaded(ImageFixtures.png())
        assertThat(service.completeUpload(owner, upload.id)).isEqualTo(ImageUploadCompletion.InProgress)
        storage.finishCopies()
        storage.copyStatusFailure = ImageStorageUnavailableException()

        assertThat(service.completeUpload(owner, upload.id)).isInstanceOf(ImageUploadCompletion.Completed::class.java)
        assertThat(images.images).hasSize(1)
    }

    @Test
    fun `복사 상태를 읽지 못하고 확정 객체도 없으면 503 이며 COPYING 과 업로드 객체를 남긴다`() {
        storage.copyCompletesImmediately = false
        val upload = uploaded(ImageFixtures.png())
        assertThat(service.completeUpload(owner, upload.id)).isEqualTo(ImageUploadCompletion.InProgress)
        storage.copyStatusFailure = ImageStorageUnavailableException()

        assertThatThrownBy { service.completeUpload(owner, upload.id) }.isInstanceOf(ImageStorageUnavailableException::class.java)

        val copying = uploads.uploads.getValue(upload.id)
        assertThat(copying.status).isEqualTo(ImageUploadStatus.COPYING)
        assertThat(copying.leaseToken).isNull()
        assertThat(storage.objects).containsKey(upload.stagingKey)
        assertThat(images.images).isEmpty()

        storage.copyStatusFailure = null
        storage.finishCopies()
        assertThat(service.completeUpload(owner, upload.id)).isInstanceOf(ImageUploadCompletion.Completed::class.java)
    }

    @Test
    fun `확정 객체 확인과 상태 조회 사이에 복사가 끝나도 실패로 보지 않고 확정한다`() {
        storage.copyCompletesImmediately = false
        val upload = uploaded(ImageFixtures.png())
        assertThat(service.completeUpload(owner, upload.id)).isEqualTo(ImageUploadCompletion.InProgress)
        storage.beforeCopyStatus = { storage.finishCopies() }

        assertThat(service.completeUpload(owner, upload.id)).isInstanceOf(ImageUploadCompletion.Completed::class.java)
        assertThat(images.images).hasSize(1)
    }

    @Test
    fun `복사가 완료라는데 확정 객체가 보이지 않으면 503 이며 COPYING 과 업로드 객체를 남긴다`() {
        storage.copyCompletesImmediately = false
        val upload = uploaded(ImageFixtures.png())
        assertThat(service.completeUpload(owner, upload.id)).isEqualTo(ImageUploadCompletion.InProgress)
        val workRequestId = uploads.uploads.getValue(upload.id).workRequestId!!
        storage.workRequests[workRequestId] = CopyStatus.COMPLETED

        assertThatThrownBy { service.completeUpload(owner, upload.id) }.isInstanceOf(ImageStorageUnavailableException::class.java)

        val copying = uploads.uploads.getValue(upload.id)
        assertThat(copying.status).isEqualTo(ImageUploadStatus.COPYING)
        assertThat(copying.leaseToken).isNull()
        assertThat(storage.objects).containsKey(upload.stagingKey)
        assertThat(images.images).isEmpty()
    }

    @Test
    fun `알고 있는 복사가 실패하고 확정 객체가 없으면 FAILED`() {
        storage.copyCompletesImmediately = false
        val upload = uploaded(ImageFixtures.png())
        service.completeUpload(owner, upload.id)
        storage.failCopies()

        assertUploadCode(ImageExceptionCode.UPLOAD_FAILED) { service.completeUpload(owner, upload.id) }
        assertThat(uploads.uploads.getValue(upload.id).status).isEqualTo(ImageUploadStatus.FAILED)
        assertThat(images.images).isEmpty()
    }

    @Test
    fun `완료 트랜잭션이 실패하면 COPYING 으로 남고 다음 호출이 이미지 하나만 만든다`() {
        val upload = uploaded(ImageFixtures.png())
        uploads.completeFailure = IllegalStateException("db down")

        assertThatThrownBy { service.completeUpload(owner, upload.id) }.hasMessage("db down")
        val copying = uploads.uploads.getValue(upload.id)
        assertThat(copying.status).isEqualTo(ImageUploadStatus.COPYING)
        assertThat(copying.leaseToken).isNull()

        uploads.completeFailure = null
        assertThat(service.completeUpload(owner, upload.id)).isInstanceOf(ImageUploadCompletion.Completed::class.java)
        assertThat(images.images).hasSize(1)
    }

    @Test
    fun `처리 중이던 서버가 죽어 lease 가 끝나면 다음 호출이 이어받는다`() {
        val upload = uploaded(ImageFixtures.png())
        uploads.save(
            upload.copyWith(status = ImageUploadStatus.VERIFYING, leaseToken = "dead", leaseUntil = clock.now.plusSeconds(30)),
        )

        assertThat(service.completeUpload(owner, upload.id)).isEqualTo(ImageUploadCompletion.InProgress)
        assertThat(storage.calls).doesNotContain("download")

        clock.now = clock.now.plusSeconds(31)
        assertThat(service.completeUpload(owner, upload.id)).isInstanceOf(ImageUploadCompletion.Completed::class.java)
    }

    @Test
    fun `lease 를 잃은 처리자는 검증 결과를 반영하지 못한다`() {
        val upload = uploaded(ImageFixtures.png())
        storage.afterDownload = {
            // 검증이 lease 보다 오래 걸려 다른 요청이 이어받았다.
            clock.now = clock.now.plus(properties.processingLease).plusSeconds(1)
            check(uploads.startVerification(upload.id, "other", clock.now, clock.now.plusSeconds(120)))
        }

        assertThat(service.completeUpload(owner, upload.id)).isEqualTo(ImageUploadCompletion.InProgress)

        val current = uploads.uploads.getValue(upload.id)
        assertThat(current.status).isEqualTo(ImageUploadStatus.VERIFYING)
        assertThat(current.leaseToken).isEqualTo("other")
        assertThat(current.etag).isNull()
        assertThat(storage.copyRequests).isEmpty()
    }

    @Test
    fun `검증은 인스턴스당 한 건이며 넘치면 429, 끝나면 다시 받는다`() {
        val first = uploaded(ImageFixtures.png())
        val second = uploaded(ImageFixtures.png())
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        storage.beforeDownload = {
            entered.countDown()
            release.await(5, TimeUnit.SECONDS)
        }

        val running = CompletableFuture.supplyAsync { service.completeUpload(owner, first.id) }
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue()
        assertThatThrownBy { service.completeUpload(owner, second.id) }.isInstanceOf(ImageVerificationBusyException::class.java)
        assertThat(uploads.uploads.getValue(second.id).status).isEqualTo(ImageUploadStatus.PENDING)

        release.countDown()
        assertThat(running.get(5, TimeUnit.SECONDS)).isInstanceOf(ImageUploadCompletion.Completed::class.java)
        storage.beforeDownload = {}
        assertThat(service.completeUpload(owner, second.id)).isInstanceOf(ImageUploadCompletion.Completed::class.java)
    }

    @Test
    fun `내려받기가 실패해도 임시 파일을 지우고 lease 와 검증 슬롯을 돌려준다`() {
        val upload = uploaded(ImageFixtures.png())
        storage.downloadFailure = ImageStorageUnavailableException()

        assertThatThrownBy { service.completeUpload(owner, upload.id) }.isInstanceOf(ImageStorageUnavailableException::class.java)
        assertTempFilesDeleted()
        assertThat(uploads.uploads.getValue(upload.id).status).isEqualTo(ImageUploadStatus.PENDING)

        storage.downloadFailure = null
        assertThat(service.completeUpload(owner, upload.id)).isInstanceOf(ImageUploadCompletion.Completed::class.java)
    }

    @Test
    fun `정리 작업은 버려진 세션을 끝내고 PAR 과 업로드 객체를 지운다`() {
        val abandoned = uploaded(ImageFixtures.png())
        val recent = service.createUpload(owner, "image/png", 10L)
        uploads.save(uploads.uploads.getValue(abandoned.id).copyWith(createdAt = clock.now.minus(Duration.ofHours(25))))

        assertThat(service.cleanUpStaleUploads()).isEqualTo(1)

        assertThat(uploads.uploads).doesNotContainKey(abandoned.id).containsKey(recent.uploadId)
        assertThat(storage.revokedPars).containsExactly(abandoned.parId)
        assertThat(storage.objects).doesNotContainKey(abandoned.stagingKey)
    }

    @Test
    fun `정리 작업은 저장소를 쓸 수 없으면 남은 건을 건드리지 않고 멈춘다`() {
        val stale =
            List(3) { uploaded(ImageFixtures.png()) }.onEach {
                uploads.save(uploads.uploads.getValue(it.id).copyWith(createdAt = clock.now.minus(Duration.ofHours(25))))
            }
        storage.calls.clear()
        storage.revokeFailure = ImageStorageUnavailableException()

        assertThat(service.cleanUpStaleUploads()).isZero()
        assertThat(storage.calls.count { it == "revoke" }).isEqualTo(1)
        assertThat(uploads.uploads.keys).containsAll(stale.map { it.id })

        storage.revokeFailure = null
        assertThat(service.cleanUpStaleUploads()).isEqualTo(3)
        assertThat(uploads.uploads.keys).doesNotContainAnyElementsOf(stale.map { it.id })
    }

    @Test
    fun `정리 작업은 완료 세션의 남은 정리만 하고 확정 이미지와 매핑은 남긴다`() {
        val upload = uploaded(ImageFixtures.png())
        storage.deleteFailure = IllegalStateException("delete down")
        service.completeUpload(owner, upload.id)
        assertThat(uploads.uploads.getValue(upload.id).parId).isNotNull()
        storage.deleteFailure = null
        clock.now = clock.now.plus(Duration.ofHours(25))

        assertThat(service.cleanUpStaleUploads()).isEqualTo(1)

        val completed = uploads.uploads.getValue(upload.id)
        assertThat(completed.status).isEqualTo(ImageUploadStatus.COMPLETED)
        assertThat(completed.parId).isNull()
        assertThat(storage.objects).containsKey(upload.finalKey).doesNotContainKey(upload.stagingKey)
        // 정리가 끝난 완료 세션은 다시 보지 않는다.
        assertThat(uploads.findStale(clock.now, 50)).isEmpty()
    }

    @Test
    fun `정리 작업은 진행 중인 복사를 지우지 않는다`() {
        storage.copyCompletesImmediately = false
        val upload = uploaded(ImageFixtures.png())
        service.completeUpload(owner, upload.id)
        clock.now = clock.now.plus(Duration.ofHours(25))

        assertThat(service.cleanUpStaleUploads()).isZero()
        assertThat(uploads.uploads.getValue(upload.id).status).isEqualTo(ImageUploadStatus.COPYING)

        storage.finishCopies()
        service.cleanUpStaleUploads()
        assertThat(uploads.uploads.getValue(upload.id).status).isEqualTo(ImageUploadStatus.COMPLETED)
        assertThat(images.images).hasSize(1)
    }

    @Test
    fun `정리 작업은 FAILED 의 늦게 생긴 확정 객체까지 지우고, 실패하면 행을 남겨 다시 시도한다`() {
        storage.copyCompletesImmediately = false
        val upload = uploaded(ImageFixtures.png())
        service.completeUpload(owner, upload.id)
        storage.failCopies()
        assertUploadCode(ImageExceptionCode.UPLOAD_FAILED) { service.completeUpload(owner, upload.id) }
        storage.put(upload.finalKey, ImageFixtures.png())
        clock.now = clock.now.plus(Duration.ofHours(25))

        storage.deleteFailure = IllegalStateException("delete down")
        assertThat(service.cleanUpStaleUploads()).isZero()
        assertThat(uploads.uploads).containsKey(upload.id)

        storage.deleteFailure = null
        assertThat(service.cleanUpStaleUploads()).isEqualTo(1)
        assertThat(uploads.uploads).doesNotContainKey(upload.id)
        assertThat(storage.objects).doesNotContainKey(upload.finalKey)
    }

    private fun uploaded(
        bytes: ByteArray,
        declaredSize: Long = bytes.size.toLong(),
        putContentType: String = "image/png",
        putContentEncoding: String? = null,
    ): ImageUpload {
        val created = service.createUpload(owner, "image/png", declaredSize)
        val upload = uploads.uploads.getValue(created.uploadId)
        storage.put(upload.stagingKey, bytes, putContentType, putContentEncoding)
        return upload
    }

    private fun assertTempFilesDeleted() {
        assertThat(storage.downloadTargets).isNotEmpty()
        assertThat(storage.downloadTargets).allSatisfy { assertThat(Files.exists(it)).isFalse() }
    }

    private fun assertCode(
        code: ImageExceptionCode,
        block: () -> Unit,
    ) {
        assertThatThrownBy { block() }
            .isInstanceOfSatisfying(InvalidImageException::class.java) { assertThat(it.getCode()).isEqualTo(code) }
    }

    private fun assertUploadCode(
        code: ImageExceptionCode,
        block: () -> Unit,
    ) {
        assertThatThrownBy { block() }
            .isInstanceOfSatisfying(ImageUploadException::class.java) { assertThat(it.getCode()).isEqualTo(code) }
    }
}
