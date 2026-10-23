package core.application.image.infrastructure

import core.application.image.application.exception.ImageStorageUnavailableException
import core.application.image.application.properties.R2StorageProperties
import core.domain.image.port.outbound.CopyStart
import core.domain.image.port.outbound.CopyStatus
import core.domain.image.port.outbound.StoredObject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.http.AbortableInputStream
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.CopyObjectResponse
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class R2ImageStorageAdapterTest {
    private val client: S3Client = mock(S3Client::class.java)
    private val presigner =
        S3Presigner
            .builder()
            .endpointOverride(URI.create("https://account.r2.cloudflarestorage.com"))
            .region(Region.of("auto"))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test-key", "test-secret")))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build()
    private val now = Instant.parse("2026-10-23T03:00:00Z")
    private val expiresAt = now.plusSeconds(600)
    private val adapter =
        R2ImageStorageAdapter(
            R2StorageProperties(accountId = "account", accessKeyId = "test-key", secretAccessKey = "test-secret", bucket = "test-bucket"),
            object : R2ClientProvider {
                override fun client() = client

                override fun presigner() = presigner
            },
            Clock.fixed(now, ZoneOffset.UTC),
        )

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `업로드 URL 은 한 객체에 대한 PUT 서명 URL 이고 만료 시각까지 유효하다`() {
        val url = adapter.createUploadUrl("uploads/u1", expiresAt)

        assertThat(url.url).startsWith("https://account.r2.cloudflarestorage.com/test-bucket/uploads/u1?")
        assertThat(url.url).contains("X-Amz-Expires=600").contains("X-Amz-Signature=")
        assertThat(url.parId).startsWith("r2-presigned-")
        assertThat(url.expiresAt).isEqualTo(expiresAt)
        assertThat(url.toString()).doesNotContain("Signature")
    }

    @Test
    fun `다운로드 URL 은 원래 파일명을 담은 attachment 응답 헤더를 서명에 포함한다`() {
        val url = adapter.createDownloadUrl("images/a", expiresAt, "사진 1.png")

        val disposition = URLDecoder.decode(url.url.substringAfter("response-content-disposition=").substringBefore("&"), Charsets.UTF_8)
        assertThat(disposition).isEqualTo("attachment; filename=\"__ 1.png\"; filename*=UTF-8''%EC%82%AC%EC%A7%84%201.png")
        assertThat(adapter.createReadUrl("images/a", expiresAt).url).doesNotContain("response-content-disposition")
    }

    @Test
    fun `서명 URL 은 회수할 수 없으므로 revoke 는 저장소를 호출하지 않는다`() {
        adapter.revokeUrl("r2-presigned-x")
        verify(client, never()).deleteObject(any(DeleteObjectRequest::class.java))
    }

    @Test
    fun `내려받기는 상한 + 1 바이트까지만 범위로 요청하고 없으면 null 이다`() {
        doReturn(objectStream(ByteArray(10) { 1 }, "\"etag\"")).`when`(client).getObject(any(GetObjectRequest::class.java))
        val target = tempDir.resolve("a")

        val downloaded = adapter.download("uploads/u1", 4, target)

        val captor = ArgumentCaptor.forClass(GetObjectRequest::class.java)
        verify(client).getObject(captor.capture())
        assertThat(captor.value.range()).isEqualTo("bytes=0-4")
        assertThat(downloaded!!.size).isEqualTo(5)
        assertThat(downloaded.etag).isEqualTo("\"etag\"")
        assertThat(Files.size(target)).isEqualTo(5)

        doThrow(s3Error(404)).`when`(client).getObject(any(GetObjectRequest::class.java))
        assertThat(adapter.download("uploads/none", 4, target)).isNull()
    }

    @Test
    fun `빈 객체는 416 뒤 HEAD 길이 0 을 확인하고 빈 파일로 본다`() {
        doThrow(s3Error(416)).`when`(client).getObject(any(GetObjectRequest::class.java))
        doReturn(HeadObjectResponse.builder().eTag("\"e\"").contentLength(0).build()).`when`(client).headObject(any(HeadObjectRequest::class.java))

        val downloaded = adapter.download("uploads/empty", 4, tempDir.resolve("e"))

        assertThat(downloaded!!.size).isZero()
    }

    @Test
    fun `복사는 대상이 없을 때 원본 ETag 조건으로 내고 즉시 완료로 본다`() {
        doThrow(s3Error(404)).`when`(client).headObject(any(HeadObjectRequest::class.java))
        doReturn(CopyObjectResponse.builder().build()).`when`(client).copyObject(any(CopyObjectRequest::class.java))

        val start = adapter.startCopy("uploads/u1", "\"etag\"", "images/u1")

        val captor = ArgumentCaptor.forClass(CopyObjectRequest::class.java)
        verify(client).copyObject(captor.capture())
        assertThat(captor.value.copySourceIfMatch()).isEqualTo("\"etag\"")
        assertThat(captor.value.sourceKey()).isEqualTo("uploads/u1")
        assertThat(captor.value.destinationKey()).isEqualTo("images/u1")
        assertThat(adapter.copyStatus((start as CopyStart.Started).workRequestId)).isEqualTo(CopyStatus.COMPLETED)
        assertThat(adapter.copyStatus("ocid1.objectstorageworkrequest")).isEqualTo(CopyStatus.IN_PROGRESS)
    }

    @Test
    fun `대상이 이미 있거나 원본이 바뀌었거나 사라지면 복사 거절이다`() {
        doReturn(HeadObjectResponse.builder().eTag("\"e\"").contentLength(3).build()).`when`(client).headObject(any(HeadObjectRequest::class.java))
        assertThat(adapter.startCopy("uploads/u1", "\"etag\"", "images/u1")).isEqualTo(CopyStart.PreconditionFailed)
        verify(client, never()).copyObject(any(CopyObjectRequest::class.java))

        doThrow(s3Error(404)).`when`(client).headObject(any(HeadObjectRequest::class.java))
        listOf(412, 404).forEach { status ->
            doThrow(s3Error(status)).`when`(client).copyObject(any(CopyObjectRequest::class.java))
            assertThat(adapter.startCopy("uploads/u1", "\"etag\"", "images/u1")).isEqualTo(CopyStart.PreconditionFailed)
        }
    }

    @Test
    fun `HEAD 는 크기와 ETag 를 주고 없으면 null, 그 밖의 오류는 503 이다`() {
        doReturn(HeadObjectResponse.builder().eTag("\"e\"").contentLength(3).build()).`when`(client).headObject(any(HeadObjectRequest::class.java))
        assertThat(adapter.head("images/a")).isEqualTo(StoredObject("\"e\"", 3))

        doThrow(s3Error(404)).`when`(client).headObject(any(HeadObjectRequest::class.java))
        assertThat(adapter.head("images/a")).isNull()

        doThrow(s3Error(403)).`when`(client).headObject(any(HeadObjectRequest::class.java))
        assertThatThrownBy { adapter.head("images/a") }.isInstanceOf(ImageStorageUnavailableException::class.java)
    }

    @Test
    fun `삭제는 이미 없어도 성공으로 본다`() {
        doThrow(s3Error(404)).`when`(client).deleteObject(any(DeleteObjectRequest::class.java))
        adapter.delete("images/a")
    }

    private fun objectStream(
        bytes: ByteArray,
        etag: String,
    ) = ResponseInputStream(
        GetObjectResponse.builder().eTag(etag).contentType("image/png").build(),
        AbortableInputStream.create(ByteArrayInputStream(bytes)),
    )

    private fun s3Error(status: Int): S3Exception = S3Exception.builder().statusCode(status).message("status $status").build() as S3Exception
}
