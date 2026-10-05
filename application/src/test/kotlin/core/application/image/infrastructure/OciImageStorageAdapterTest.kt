package core.application.image.infrastructure

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails
import com.oracle.bmc.objectstorage.model.PreauthenticatedRequest
import com.oracle.bmc.objectstorage.model.WorkRequest
import com.oracle.bmc.objectstorage.requests.CopyObjectRequest
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest
import com.oracle.bmc.objectstorage.requests.DeletePreauthenticatedRequestRequest
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.responses.CopyObjectResponse
import com.oracle.bmc.objectstorage.responses.CreatePreauthenticatedRequestResponse
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import com.oracle.bmc.objectstorage.responses.GetWorkRequestResponse
import com.oracle.bmc.objectstorage.responses.HeadObjectResponse
import core.application.image.application.exception.ImageStorageUnavailableException
import core.application.image.application.properties.ImageStorageProperties
import core.domain.image.port.outbound.CopyStart
import core.domain.image.port.outbound.CopyStatus
import core.domain.image.port.outbound.StoredObject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.Date

class OciImageStorageAdapterTest {
    private val client: ObjectStorage = mock(ObjectStorage::class.java)
    private val properties = ImageStorageProperties(region = "ap-seoul-1", namespace = "test-ns", bucket = "test-bucket")
    private val adapter = OciImageStorageAdapter(properties) { client }
    private val expiresAt = Instant.parse("2026-10-04T03:10:00Z")

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `업로드 URL 은 한 객체에 대한 쓰기 PAR 이며 목록 조회를 막는다`() {
        doReturn(parResponse("https://objectstorage.ap-seoul-1.oraclecloud.com/p/token/n/test-ns/b/test-bucket/o/uploads/u1"))
            .`when`(client)
            .createPreauthenticatedRequest(any())

        val url = adapter.createUploadUrl("uploads/u1", expiresAt)

        val captor = ArgumentCaptor.forClass(CreatePreauthenticatedRequestRequest::class.java)
        verify(client).createPreauthenticatedRequest(captor.capture())
        val details = captor.value.createPreauthenticatedRequestDetails
        assertThat(captor.value.namespaceName).isEqualTo("test-ns")
        assertThat(captor.value.bucketName).isEqualTo("test-bucket")
        assertThat(details.objectName).isEqualTo("uploads/u1")
        assertThat(details.accessType).isEqualTo(CreatePreauthenticatedRequestDetails.AccessType.ObjectWrite)
        assertThat(details.bucketListingAction).isEqualTo(PreauthenticatedRequest.BucketListingAction.Deny)
        assertThat(details.timeExpires.toInstant()).isEqualTo(expiresAt)
        assertThat(url.parId).isEqualTo("par-id")
        assertThat(url.url).endsWith("/o/uploads/u1")
        assertThat(url.expiresAt).isEqualTo(expiresAt)
        assertThat(url.toString()).doesNotContain("token")
    }

    @Test
    fun `조회 URL 은 읽기 PAR 이며 fullPath 가 없으면 엔드포인트와 accessUri 로 만든다`() {
        doReturn(parResponse(null, accessUri = "/p/token/n/test-ns/b/test-bucket/o/images/a"))
            .`when`(client)
            .createPreauthenticatedRequest(any())
        doReturn("https://objectstorage.ap-seoul-1.oraclecloud.com").`when`(client).endpoint

        val url = adapter.createReadUrl("images/a", expiresAt)

        val captor = ArgumentCaptor.forClass(CreatePreauthenticatedRequestRequest::class.java)
        verify(client).createPreauthenticatedRequest(captor.capture())
        assertThat(captor.value.createPreauthenticatedRequestDetails.accessType)
            .isEqualTo(CreatePreauthenticatedRequestDetails.AccessType.ObjectRead)
        assertThat(url.url).isEqualTo("https://objectstorage.ap-seoul-1.oraclecloud.com/p/token/n/test-ns/b/test-bucket/o/images/a")
    }

    @Test
    fun `PAR 회수는 이미 없으면 성공, 그 외 오류는 503`() {
        doThrow(BmcException(404, "NotFound", "gone", "req-1")).`when`(client).deletePreauthenticatedRequest(any())
        assertThatCode { adapter.revokeUrl("par-id") }.doesNotThrowAnyException()
        val captor = ArgumentCaptor.forClass(DeletePreauthenticatedRequestRequest::class.java)
        verify(client).deletePreauthenticatedRequest(captor.capture())
        assertThat(captor.value.parId).isEqualTo("par-id")

        doThrow(BmcException(500, "InternalServerError", "boom", "req-2")).`when`(client).deletePreauthenticatedRequest(any())
        assertThatThrownBy { adapter.revokeUrl("par-id") }.isInstanceOf(ImageStorageUnavailableException::class.java)
    }

    @Test
    fun `내려받기는 파일로 쓰고 ETag 와 헤더를 돌려주며 스트림을 닫는다`() {
        val body = TrackingInputStream(byteArrayOf(9, 8, 7))
        doReturn(getResponse(body, 3L)).`when`(client).getObject(any())
        val target = tempDir.resolve("download")

        val downloaded = adapter.download("uploads/u1", 3L, target)!!

        assertThat(Files.readAllBytes(target)).containsExactly(9, 8, 7)
        assertThat(downloaded.etag).isEqualTo("etag-1")
        assertThat(downloaded.size).isEqualTo(3L)
        assertThat(downloaded.contentType).isEqualTo("image/png")
        assertThat(downloaded.contentEncoding).isNull()
        assertThat(body.closed).isTrue()
    }

    @Test
    fun `내려받기는 0 부터 상한까지(포함) 범위만 요청해 상한을 넘는 객체도 상한 + 1 바이트만 전송받는다`() {
        // 범위를 지킨 응답: 10 바이트 객체에서 0..5 만 온다.
        val body = TrackingInputStream(ByteArray(6))
        doReturn(getResponse(body, 6L)).`when`(client).getObject(any())
        val target = tempDir.resolve("download")

        val downloaded = adapter.download("uploads/u1", 5L, target)!!

        val captor = ArgumentCaptor.forClass(GetObjectRequest::class.java)
        verify(client).getObject(captor.capture())
        assertThat(captor.value.range.startByte).isEqualTo(0L)
        assertThat(captor.value.range.endByte).isEqualTo(5L)
        assertThat(downloaded.size).isEqualTo(6L)
        assertThat(Files.size(target)).isEqualTo(6L)
        assertThat(body.closed).isTrue()
    }

    @Test
    fun `범위 요청이 416 이면 HEAD 로 길이 0 을 확인한 경우에만 빈 객체로 본다`() {
        doThrow(BmcException(416, "InvalidRange", "range", "req-10")).`when`(client).getObject(any())
        doReturn(HeadObjectResponse.builder().eTag("etag-empty").contentLength(0L).contentType("image/png").build())
            .`when`(client)
            .headObject(any())
        val target = tempDir.resolve("empty")

        val downloaded = adapter.download("uploads/u1", 5L, target)!!

        assertThat(downloaded.etag).isEqualTo("etag-empty")
        assertThat(downloaded.size).isZero()
        assertThat(downloaded.contentType).isEqualTo("image/png")
        assertThat(Files.size(target)).isZero()

        // 길이가 0 이 아닌데 416 이면 빈 파일로 넘기지 않는다.
        doReturn(HeadObjectResponse.builder().eTag("etag-x").contentLength(7L).build()).`when`(client).headObject(any())
        assertThatThrownBy { adapter.download("uploads/u1", 5L, tempDir.resolve("x")) }
            .isInstanceOf(ImageStorageUnavailableException::class.java)

        // 그 사이 지워졌으면 없는 객체다.
        doThrow(BmcException(404, "ObjectNotFound", "missing", "req-11")).`when`(client).headObject(any())
        assertThat(adapter.download("uploads/u1", 5L, tempDir.resolve("y"))).isNull()
    }

    @Test
    fun `서버가 범위를 무시해도 상한 + 1 바이트까지만 쓴다`() {
        val body = TrackingInputStream(ByteArray(10))
        doReturn(getResponse(body, null)).`when`(client).getObject(any())
        val target = tempDir.resolve("download")

        assertThat(adapter.download("uploads/u1", 5L, target)!!.size).isEqualTo(6L)
        assertThat(Files.size(target)).isEqualTo(6L)
        assertThat(body.readCount).isEqualTo(6)
        assertThat(body.closed).isTrue()
    }

    @Test
    fun `없는 객체는 null, 그 외 오류와 클라이언트 생성 실패는 503`() {
        doThrow(BmcException(404, "ObjectNotFound", "missing", "req-3")).`when`(client).getObject(any())
        assertThat(adapter.download("uploads/u1", 5L, tempDir.resolve("a"))).isNull()

        doThrow(BmcException(404, "ObjectNotFound", "missing", "req-4")).`when`(client).headObject(any())
        assertThat(adapter.head("images/u1")).isNull()

        doThrow(BmcException(500, "InternalServerError", "boom", "req-5")).`when`(client).getObject(any())
        assertThatThrownBy { adapter.download("uploads/u1", 5L, tempDir.resolve("b")) }
            .isInstanceOf(ImageStorageUnavailableException::class.java)

        val unavailable = OciImageStorageAdapter(properties) { throw ImageStorageUnavailableException() }
        assertThatThrownBy { unavailable.head("images/u1") }.isInstanceOf(ImageStorageUnavailableException::class.java)
    }

    @Test
    fun `head 는 ETag 와 크기를 돌려준다`() {
        doReturn(HeadObjectResponse.builder().eTag("etag-2").contentLength(42L).build()).`when`(client).headObject(any())

        assertThat(adapter.head("images/u1")).isEqualTo(StoredObject("etag-2", 42L))
    }

    @Test
    fun `복사는 검증한 원본 ETag 와 대상 if-none-match 를 건다`() {
        doReturn(CopyObjectResponse.builder().opcWorkRequestId("wr-1").build()).`when`(client).copyObject(any())

        assertThat(adapter.startCopy("uploads/u1", "etag-1", "images/u1")).isEqualTo(CopyStart.Started("wr-1"))

        val captor = ArgumentCaptor.forClass(CopyObjectRequest::class.java)
        verify(client).copyObject(captor.capture())
        val details = captor.value.copyObjectDetails
        assertThat(captor.value.bucketName).isEqualTo("test-bucket")
        assertThat(details.sourceObjectName).isEqualTo("uploads/u1")
        assertThat(details.sourceObjectIfMatchETag).isEqualTo("etag-1")
        assertThat(details.destinationRegion).isEqualTo("ap-seoul-1")
        assertThat(details.destinationNamespace).isEqualTo("test-ns")
        assertThat(details.destinationBucket).isEqualTo("test-bucket")
        assertThat(details.destinationObjectName).isEqualTo("images/u1")
        assertThat(details.destinationObjectIfNoneMatchETag).isEqualTo("*")
    }

    @Test
    fun `복사 조건 불일치는 거절, 응답을 못 받은 실패는 503`() {
        doThrow(BmcException(412, "IfMatchFailed", "etag", "req-6")).`when`(client).copyObject(any())
        assertThat(adapter.startCopy("uploads/u1", "etag-1", "images/u1")).isEqualTo(CopyStart.PreconditionFailed)

        doThrow(BmcException(409, "IfNoneMatchFailed", "exists", "req-12")).`when`(client).copyObject(any())
        assertThat(adapter.startCopy("uploads/u1", "etag-1", "images/u1")).isEqualTo(CopyStart.PreconditionFailed)

        // 권한 없음도 404 라 거절로 보지 않고 503 으로 낸다.
        doThrow(BmcException(404, "NotAuthorizedOrNotFound", "missing", "req-7")).`when`(client).copyObject(any())
        assertThatThrownBy { adapter.startCopy("uploads/u1", "etag-1", "images/u1") }
            .isInstanceOf(ImageStorageUnavailableException::class.java)

        doThrow(BmcException(true, "timeout", RuntimeException(), "req-8")).`when`(client).copyObject(any())
        assertThatThrownBy { adapter.startCopy("uploads/u1", "etag-1", "images/u1") }
            .isInstanceOf(ImageStorageUnavailableException::class.java)
    }

    @Test
    fun `work request 상태를 복사 상태로 바꾼다`() {
        fun statusOf(status: WorkRequest.Status?): CopyStatus {
            doReturn(GetWorkRequestResponse.builder().workRequest(WorkRequest.builder().status(status).build()).build())
                .`when`(client)
                .getWorkRequest(any())
            return adapter.copyStatus("wr-1")
        }

        assertThat(statusOf(WorkRequest.Status.Accepted)).isEqualTo(CopyStatus.IN_PROGRESS)
        assertThat(statusOf(WorkRequest.Status.InProgress)).isEqualTo(CopyStatus.IN_PROGRESS)
        assertThat(statusOf(WorkRequest.Status.Completed)).isEqualTo(CopyStatus.COMPLETED)
        assertThat(statusOf(WorkRequest.Status.Failed)).isEqualTo(CopyStatus.FAILED)
        assertThat(statusOf(WorkRequest.Status.Canceled)).isEqualTo(CopyStatus.FAILED)
        assertThat(statusOf(WorkRequest.Status.UnknownEnumValue)).isEqualTo(CopyStatus.IN_PROGRESS)
    }

    @Test
    fun `work request 404 는 실패로 단정하지 않고 503 이다`() {
        doThrow(BmcException(404, "NotAuthorizedOrNotFound", "missing", "req-13")).`when`(client).getWorkRequest(any())

        assertThatThrownBy { adapter.copyStatus("wr-1") }.isInstanceOf(ImageStorageUnavailableException::class.java)
    }

    @Test
    fun `delete 는 이미 없는 객체를 성공으로 본다`() {
        doThrow(BmcException(404, "ObjectNotFound", "missing", "req-9")).`when`(client).deleteObject(any())

        assertThatCode { adapter.delete("uploads/u1") }.doesNotThrowAnyException()
        val captor = ArgumentCaptor.forClass(DeleteObjectRequest::class.java)
        verify(client).deleteObject(captor.capture())
        assertThat(captor.value.objectName).isEqualTo("uploads/u1")
    }

    private fun parResponse(
        fullPath: String?,
        accessUri: String = "/p/token/n/test-ns/b/test-bucket/o/x",
    ): CreatePreauthenticatedRequestResponse =
        CreatePreauthenticatedRequestResponse
            .builder()
            .preauthenticatedRequest(
                PreauthenticatedRequest
                    .builder()
                    .id("par-id")
                    .accessUri(accessUri)
                    .fullPath(fullPath)
                    .timeExpires(Date.from(expiresAt))
                    .build(),
            ).build()

    private fun getResponse(
        body: TrackingInputStream,
        contentLength: Long?,
    ): GetObjectResponse =
        GetObjectResponse
            .builder()
            .inputStream(body)
            .eTag("etag-1")
            .contentType("image/png")
            .contentLength(contentLength)
            .build()

    private class TrackingInputStream(
        bytes: ByteArray,
    ) : ByteArrayInputStream(bytes) {
        var closed = false
        var readCount = 0

        override fun read(
            b: ByteArray,
            off: Int,
            len: Int,
        ): Int = super.read(b, off, len).also { if (it > 0) readCount += it }

        override fun close() {
            closed = true
            super.close()
        }
    }
}
