package core.it.image

import core.domain.image.aggregate.Image
import core.domain.image.aggregate.ImageUpload
import core.domain.image.enums.ImageContentType
import core.domain.image.enums.ImageUploadStatus
import core.domain.member.vo.MemberId
import core.persistence.image.repository.ImageUploadRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * 실제 MySQL 에서 업로드 세션의 조건부 UPDATE·완료 트랜잭션·UNIQUE 를 검증한다. 기본 test 태스크에서는 제외되며 스키마를 새로 만든다.
 *
 *   DPM_IT_MYSQL_URL='jdbc:mysql://127.0.0.1:3307/dpm_it' DPM_IT_MYSQL_USERNAME=root DPM_IT_MYSQL_PASSWORD=it \
 *   ./gradlew :application:mysqlIntegrationTest
 */
@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = ImageUploadMySqlIntegrationTest.URL_ENV, matches = ".+")
@SpringBootTest(
    classes = [ImageUploadMySqlIntegrationTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
class ImageUploadMySqlIntegrationTest {
    @Autowired lateinit var repository: ImageUploadRepository

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    private val now = Instant.parse("2026-10-04T03:00:00Z")
    private val lease = now.plusSeconds(120)

    @BeforeEach
    fun clean() {
        jdbcTemplate.update("DELETE FROM image_uploads")
        jdbcTemplate.update("DELETE FROM images")
    }

    @Test
    fun `검증은 한 처리자만 시작하고, lease 가 끝나야 다른 처리자가 이어받는다`() {
        val upload = save()

        assertThat(repository.startVerification(upload.id, "a", now, lease)).isTrue()
        assertThat(repository.startVerification(upload.id, "b", now.plusSeconds(60), now.plusSeconds(180))).isFalse()
        assertThat(repository.startVerification(upload.id, "b", lease, lease.plusSeconds(120))).isTrue()

        // lease 를 잃은 a 는 아무것도 반영하지 못한다.
        assertThat(repository.markValidated(upload.id, "a", "etag", lease)).isFalse()
        assertThat(repository.reject(upload.id, "a", "INVALID_IMAGE")).isFalse()
        assertThat(repository.releaseVerification(upload.id, "a")).isFalse()
        assertThat(repository.findById(upload.id)!!.leaseToken).isEqualTo("b")
    }

    @Test
    fun `업로드 URL 이 만료되면 검증을 시작하지 않는다`() {
        val upload = save()

        assertThat(repository.startVerification(upload.id, "a", upload.expiresAt, upload.expiresAt.plusSeconds(120))).isFalse()
        assertThat(repository.findById(upload.id)!!.status).isEqualTo(ImageUploadStatus.PENDING)
    }

    @Test
    fun `완료는 이미지 행과 세션 상태를 함께 커밋하고, token 이 틀리면 이미지 행까지 되돌린다`() {
        val upload = copying()

        assertThat(repository.complete(upload.id, "stale", image(upload))).isNull()
        assertThat(count("images")).isZero()
        assertThat(repository.findById(upload.id)!!.status).isEqualTo(ImageUploadStatus.COPYING)

        val saved = repository.complete(upload.id, "a", image(upload))!!
        val completed = repository.findById(upload.id)!!
        assertThat(completed.status).isEqualTo(ImageUploadStatus.COMPLETED)
        assertThat(completed.imageId).isEqualTo(saved.id)
        assertThat(completed.leaseToken).isNull()

        // 같은 세션으로 다시 완료해도 상태 조건에 막힌다.
        assertThat(repository.complete(upload.id, "a", image(upload))).isNull()
        assertThat(count("images")).isEqualTo(1)
    }

    @Test
    fun `같은 확정 키로 이미지 행이 둘 생기지 않는다`() {
        val upload = copying()
        repository.complete(upload.id, "a", image(upload))!!
        val other = copying()

        assertThatThrownBy { repository.complete(other.id, "a", image(upload)) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
        assertThat(repository.findById(other.id)!!.status).isEqualTo(ImageUploadStatus.COPYING)
        assertThat(count("images")).isEqualTo(1)
    }

    @Test
    fun `복사 lease 는 비었거나 끝났을 때만 가져오고 work request id 는 소유자만 기록한다`() {
        val upload = copying()
        assertThat(repository.acquireCopy(upload.id, "b", now, lease)).isFalse()
        assertThat(repository.releaseCopy(upload.id, "a")).isTrue()
        assertThat(repository.acquireCopy(upload.id, "b", now, lease)).isTrue()

        assertThat(repository.recordCopyWorkRequest(upload.id, "a", "wr-a")).isFalse()
        assertThat(repository.recordCopyWorkRequest(upload.id, "b", "wr-b")).isTrue()
        assertThat(repository.findById(upload.id)!!.workRequestId).isEqualTo("wr-b")
        assertThat(repository.fail(upload.id, "a", "UPLOAD_FAILED")).isFalse()
        assertThat(repository.fail(upload.id, "b", "UPLOAD_FAILED")).isTrue()
    }

    @Test
    fun `정리 대상은 오래되고 parId 가 남은 행이며, lease 가 살아 있는 세션은 만료시키지 않는다`() {
        val old = now.minusSeconds(90_000)
        val pending = save(createdAt = old)
        val verifying = save(createdAt = old)
        check(repository.startVerification(verifying.id, "a", old, lease))
        val completed = copying(createdAt = old)
        repository.complete(completed.id, "a", image(completed))
        repository.clearParId(completed.id)
        save(createdAt = now)

        assertThat(repository.findStale(now.minusSeconds(86_400), 50).map { it.id })
            .containsExactlyInAnyOrder(pending.id, verifying.id)
        assertThat(repository.expire(verifying.id, now)).isFalse()
        assertThat(repository.expire(pending.id, now)).isTrue()
        assertThat(repository.deleteTerminal(verifying.id)).isFalse()
        assertThat(repository.deleteTerminal(pending.id)).isTrue()
        assertThat(repository.deleteTerminal(completed.id)).isFalse()
    }

    private fun save(createdAt: Instant = now): ImageUpload =
        repository.save(
            ImageUpload.create(
                id = UUID.randomUUID().toString(),
                ownerMemberId = MemberId(7L),
                contentType = ImageContentType.PNG,
                size = 10L,
                parId = "par-${UUID.randomUUID()}",
                expiresAt = createdAt.plusSeconds(600).truncatedTo(ChronoUnit.MICROS),
                createdAt = createdAt.truncatedTo(ChronoUnit.MICROS),
            ),
        )

    /** 검증을 마치고 token "a" 로 복사 lease 를 가진 세션. */
    private fun copying(createdAt: Instant = now): ImageUpload {
        val upload = save(createdAt)
        check(repository.startVerification(upload.id, "a", createdAt, createdAt.plusSeconds(120)))
        check(repository.markValidated(upload.id, "a", "etag", createdAt.plusSeconds(120)))
        return upload
    }

    private fun image(upload: ImageUpload): Image = Image.create(upload.ownerMemberId, upload.finalKey, upload.contentType, upload.size, now)

    private fun count(table: String): Int = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $table", Int::class.java)!!

    companion object {
        const val URL_ENV = "DPM_IT_MYSQL_URL"
        private const val USERNAME_ENV = "DPM_IT_MYSQL_USERNAME"
        private const val PASSWORD_ENV = "DPM_IT_MYSQL_PASSWORD"

        private val SAFE_URL = Regex("^jdbc:mysql://(localhost|127\\.0\\.0\\.1)(:\\d+)?/dpm_it[A-Za-z0-9_]*(\\?.*)?$")

        @JvmStatic
        @BeforeAll
        fun requireDisposableLocalDatabase() {
            assumeTrue(SAFE_URL.matches(System.getenv(URL_ENV).orEmpty())) {
                "$URL_ENV 는 로컬 일회용 DB(jdbc:mysql://localhost|127.0.0.1/dpm_it*)만 허용합니다"
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) {
            val url = System.getenv(URL_ENV).orEmpty()
            require(SAFE_URL.matches(url)) { "$URL_ENV 는 로컬 일회용 DB(dpm_it*)만 허용합니다: $url" }
            registry.add("spring.datasource.url") { url }
            registry.add("spring.datasource.username") { System.getenv(USERNAME_ENV) ?: "root" }
            registry.add("spring.datasource.password") { System.getenv(PASSWORD_ENV) ?: "" }
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create" }
            registry.add("spring.jpa.properties.hibernate.show_sql") { "false" }
        }
    }
}
