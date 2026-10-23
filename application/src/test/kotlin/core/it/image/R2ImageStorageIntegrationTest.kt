package core.it.image

import core.application.image.FakeImagePersistencePort
import core.application.image.FakeImageUploadPersistencePort
import core.application.image.ImageFixtures
import core.application.image.application.dto.ImageUploadCompletion
import core.application.image.application.properties.ImageStorageProperties
import core.application.image.application.properties.R2StorageProperties
import core.application.image.application.service.ImageCommandService
import core.application.image.application.service.ImageQueryService
import core.application.image.application.validator.ImageValidator
import core.application.image.infrastructure.LazyR2ClientProvider
import core.application.image.infrastructure.R2ImageStorageAdapter
import core.domain.image.vo.ImageId
import core.domain.member.vo.MemberId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Clock

/**
 * 실제 S3 호환 저장소(R2 또는 로컬 MinIO)에서 업로드 URL 발급 → 프론트처럼 PUT → complete(검증·복사) → 읽기 URL 로 GET 까지 확인한다.
 * 버킷에 객체를 만들고 지우므로 테스트용 버킷만 쓴다.
 *
 *   DPM_IT_R2_ENDPOINT=http://127.0.0.1:9000 DPM_IT_R2_ACCESS_KEY_ID=... DPM_IT_R2_SECRET_ACCESS_KEY=... DPM_IT_R2_BUCKET=dpm-it \
 *   ./gradlew :application:test --tests '*R2ImageStorageIntegrationTest'
 */
@Tag("storage-integration")
@EnabledIfEnvironmentVariable(named = "DPM_IT_R2_BUCKET", matches = ".+")
class R2ImageStorageIntegrationTest {
    private val r2Properties =
        R2StorageProperties(
            accountId = System.getenv("DPM_IT_R2_ACCOUNT_ID").orEmpty(),
            accessKeyId = System.getenv("DPM_IT_R2_ACCESS_KEY_ID").orEmpty(),
            secretAccessKey = System.getenv("DPM_IT_R2_SECRET_ACCESS_KEY").orEmpty(),
            bucket = System.getenv("DPM_IT_R2_BUCKET").orEmpty(),
            endpoint = System.getenv("DPM_IT_R2_ENDPOINT").orEmpty(),
        )
    private val clientProvider = LazyR2ClientProvider(r2Properties)
    private val clock = Clock.systemUTC()
    private val storage = R2ImageStorageAdapter(r2Properties, clientProvider, clock)
    private val images = FakeImagePersistencePort()
    private val uploads = FakeImageUploadPersistencePort(images)
    private val properties = ImageStorageProperties()
    private val commandService = ImageCommandService(ImageValidator(), storage, uploads, properties, clock)
    private val queryService = ImageQueryService(images, storage, properties, clock)
    private val http = HttpClient.newHttpClient()
    private val member = MemberId(1)

    @AfterEach
    fun cleanUp() {
        images.images.values.forEach { storage.delete(it.objectKey) }
        clientProvider.destroy()
    }

    @Test
    fun `프론트가 서명 URL 로 올린 이미지를 검증·복사해 확정하고 읽기·다운로드 URL 로 내려받는다`() {
        val bytes = ImageFixtures.png(8, 6)
        val created = commandService.createUpload(member, "image/png", bytes.size.toLong(), "사진.png")

        val put =
            http.send(
                HttpRequest
                    .newBuilder(URI.create(created.uploadUrl))
                    .header("Content-Type", "image/png")
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
                    .build(),
                HttpResponse.BodyHandlers.discarding(),
            )
        assertThat(put.statusCode()).isEqualTo(200)

        val completion = commandService.completeUpload(member, created.uploadId)
        assertThat(completion).isInstanceOf(ImageUploadCompletion.Completed::class.java)
        val imageId = ImageId((completion as ImageUploadCompletion.Completed).image.imageId)
        // 확정 뒤에는 업로드 객체를 지운다.
        assertThat(storage.head("uploads/${created.uploadId}")).isNull()
        // 같은 complete 를 다시 불러도 같은 이미지다.
        assertThat((commandService.completeUpload(member, created.uploadId) as ImageUploadCompletion.Completed).image.imageId)
            .isEqualTo(imageId.value)

        val read = get(queryService.getImage(member, imageId).url)
        assertThat(read.statusCode()).isEqualTo(200)
        assertThat(read.body()).isEqualTo(bytes)

        val download = get(queryService.getDownloadUrl(member, imageId).url)
        assertThat(download.statusCode()).isEqualTo(200)
        assertThat(download.headers().firstValue("Content-Disposition").orElse(""))
            .startsWith("attachment;")
            .contains("filename*=UTF-8''%EC%82%AC%EC%A7%84.png")
    }

    @Test
    fun `이미지가 아닌 파일은 거절하고 확정 키를 만들지 않는다`() {
        val bytes = "not an image".toByteArray()
        val created = commandService.createUpload(member, "image/png", bytes.size.toLong())
        http.send(
            HttpRequest.newBuilder(URI.create(created.uploadUrl)).PUT(HttpRequest.BodyPublishers.ofByteArray(bytes)).build(),
            HttpResponse.BodyHandlers.discarding(),
        )

        runCatching { commandService.completeUpload(member, created.uploadId) }
        assertThat(storage.head("images/${created.uploadId}")).isNull()
        assertThat(images.images).isEmpty()
        storage.delete("uploads/${created.uploadId}")
    }

    private fun get(url: String) = http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray())
}
