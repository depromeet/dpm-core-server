package core.application.image.infrastructure

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.ObjectStorage
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import com.oracle.bmc.objectstorage.responses.GetObjectResponse
import com.oracle.bmc.objectstorage.responses.PutObjectResponse
import core.application.image.application.exception.ImageNotFoundException
import core.application.image.application.exception.ImageStorageUnavailableException
import core.application.image.application.properties.ImageStorageProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.io.ByteArrayInputStream

class OciImageStorageAdapterTest {
    private val client: ObjectStorage = mock(ObjectStorage::class.java)
    private val properties = ImageStorageProperties(region = "ap-seoul-1", namespace = "test-ns", bucket = "test-bucket")
    private val adapter = OciImageStorageAdapter(properties) { client }

    @Test
    fun `put 은 버킷 좌표와 길이, 형식, 덮어쓰기 금지를 실어 보낸다`() {
        doReturn(PutObjectResponse.builder().build()).`when`(client).putObject(any())

        adapter.put("images/key", byteArrayOf(1, 2, 3), "image/png")

        val captor = ArgumentCaptor.forClass(PutObjectRequest::class.java)
        verify(client).putObject(captor.capture())
        val request = captor.value
        assertThat(request.namespaceName).isEqualTo("test-ns")
        assertThat(request.bucketName).isEqualTo("test-bucket")
        assertThat(request.objectName).isEqualTo("images/key")
        assertThat(request.contentLength).isEqualTo(3L)
        assertThat(request.contentType).isEqualTo("image/png")
        assertThat(request.ifNoneMatch).isEqualTo("*")
        assertThat(request.putObjectBody.readAllBytes()).containsExactly(1, 2, 3)
    }

    @Test
    fun `put 오류는 404 여도 503 으로 바꾼다`() {
        doThrow(BmcException(404, "BucketNotFound", "no bucket", "req-1")).`when`(client).putObject(any())

        assertThatThrownBy { adapter.put("images/key", byteArrayOf(1), "image/png") }
            .isInstanceOf(ImageStorageUnavailableException::class.java)
    }

    @Test
    fun `get 은 바이트를 읽고 스트림을 닫는다`() {
        val body = TrackingInputStream(byteArrayOf(9, 8, 7))
        doReturn(response(body, 3L)).`when`(client).getObject(any())

        assertThat(adapter.get("images/key", 3L)).containsExactly(9, 8, 7)
        assertThat(body.closed).isTrue()

        val captor = ArgumentCaptor.forClass(GetObjectRequest::class.java)
        verify(client).getObject(captor.capture())
        assertThat(captor.value.objectName).isEqualTo("images/key")
        assertThat(captor.value.bucketName).isEqualTo("test-bucket")
    }

    @Test
    fun `Content-Length 가 상한을 넘으면 읽지 않고 닫은 뒤 503`() {
        val body = TrackingInputStream(ByteArray(10))
        doReturn(response(body, 10L)).`when`(client).getObject(any())

        assertThatThrownBy { adapter.get("images/key", 5L) }.isInstanceOf(ImageStorageUnavailableException::class.java)
        assertThat(body.readCount).isZero()
        assertThat(body.closed).isTrue()
    }

    @Test
    fun `길이 헤더가 없어도 상한 + 1 바이트까지만 읽고 503`() {
        val body = TrackingInputStream(ByteArray(10))
        doReturn(response(body, null)).`when`(client).getObject(any())

        assertThatThrownBy { adapter.get("images/key", 5L) }.isInstanceOf(ImageStorageUnavailableException::class.java)
        assertThat(body.readCount).isLessThanOrEqualTo(6)
        assertThat(body.closed).isTrue()
    }

    @Test
    fun `get 404 는 이미지 없음, 그 외 오류와 클라이언트 생성 실패는 503`() {
        doThrow(BmcException(404, "ObjectNotFound", "missing", "req-2")).`when`(client).getObject(any())
        assertThatThrownBy { adapter.get("images/key", 5L) }.isInstanceOf(ImageNotFoundException::class.java)

        doThrow(BmcException(500, "InternalServerError", "boom", "req-3")).`when`(client).getObject(any())
        assertThatThrownBy { adapter.get("images/key", 5L) }.isInstanceOf(ImageStorageUnavailableException::class.java)

        val unavailable = OciImageStorageAdapter(properties) { throw ImageStorageUnavailableException() }
        assertThatThrownBy { unavailable.get("images/key", 5L) }.isInstanceOf(ImageStorageUnavailableException::class.java)
    }

    @Test
    fun `delete 는 이미 없는 객체를 성공으로 본다`() {
        doThrow(BmcException(404, "ObjectNotFound", "missing", "req-4")).`when`(client).deleteObject(any())

        assertThatCode { adapter.delete("images/key") }.doesNotThrowAnyException()
        val captor = ArgumentCaptor.forClass(DeleteObjectRequest::class.java)
        verify(client).deleteObject(captor.capture())
        assertThat(captor.value.objectName).isEqualTo("images/key")
    }

    private fun response(
        body: TrackingInputStream,
        contentLength: Long?,
    ): GetObjectResponse =
        GetObjectResponse
            .builder()
            .inputStream(body)
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
