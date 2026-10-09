package core.it.member

import core.application.common.configuration.JooqDslConfig
import core.application.common.exception.BusinessException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.service.MemberProfileService
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.MemberProfilePersistencePort
import core.domain.member.vo.MemberId
import core.it.attendance.AttendanceConcurrencyMySqlIntegrationTest
import core.persistence.member.repository.MemberProfileRepository
import core.persistence.member.repository.MemberRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = "DPM_IT_MYSQL_URL", matches = ".+")
@SpringBootTest(classes = [MemberProfileMySqlIntegrationTest.Config::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MemberProfileMySqlIntegrationTest {
    @Autowired lateinit var profiles: MemberProfilePersistencePort

    @Autowired lateinit var members: MemberPersistencePort

    @Autowired lateinit var jdbc: JdbcTemplate

    @Autowired lateinit var tx: PlatformTransactionManager

    private val service get() = MemberProfileService(profiles)

    @BeforeEach
    fun fixture() {
        listOf("member_oauth", "member_cohorts", "members").forEach { jdbc.execute("delete from $it") }
        jdbc.update("insert into members (member_id, name, signup_email, part, status, created_at) values (1, 'nickname', 'profile@example.com', 'SERVER', 'ACTIVE', now(6))")
    }

    @Test
    fun `과거 잘못된 파트는 보완 조회에서 미배정으로 제공하고 최초 입력으로 수정한다`() {
        listOf("", " ", "web", "UNASSIGNED", "UNKNOWN").forEach { part ->
            jdbc.update("update members set part=?,profile_completed_at=null where member_id=1", part)
            assertThat(service.get(1).part).isNull()
            assertThat(service.get(1).profileCompletionRequired).isTrue()
            rc().execute { service.complete(1, "홍길동", "SERVER") }
            assertThat(service.get(1).part).isEqualTo("SERVER")
            assertThat(service.get(1).profileCompletionRequired).isFalse()
        }
    }

    @Test
    fun `이름만 불량인 승인 회원도 두 값을 저장하고 일반 저장과 관리자 미배정은 완료를 되돌리지 않는다`() {
        val stale = rc().execute { members.findById(MemberId(1))!! }!!
        rc().execute { service.complete(1, "홍 길동", "WEB") }
        val completedAt = profiles.findProfile(1)!!.completedAt
        assertThat(completedAt).isNotNull()
        assertThat(jdbc.queryForMap("select name, part, status from members where member_id=1"))
            .containsEntry("name", "홍 길동").containsEntry("part", "WEB").containsEntry("status", "ACTIVE")
        val fresh = rc().execute { members.findById(MemberId(1))!! }!!
        assertThat(fresh.profileCompletedAt).isEqualTo(completedAt)
        rc().execute { members.save(stale) }
        assertThat(profiles.findProfile(1)!!.completedAt).isEqualTo(completedAt)
        rc().execute { members.updateManagementFields(listOf(1), true, null, null, emptySet()) }
        assertThat(service.get(1).profileCompletionRequired).isFalse()
        assertThatThrownBy { rc().execute { service.complete(1, "홍길동", "SERVER") } }.isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `같은 값 재시도는 완료시각과 갱신시각을 유지하고 Apple 우회도 차단한다`() {
        jdbc.update("insert into member_oauth (member_id, external_id, provider) values (1, 'test-apple', 'APPLE')")
        rc().execute { service.complete(1, "홍길동", "WEB") }
        val before = jdbc.queryForMap("select updated_at, profile_completed_at from members where member_id=1")
        rc().execute { service.complete(1, " 홍길동 ", "WEB", appleOnly = true) }
        assertThat(jdbc.queryForMap("select updated_at, profile_completed_at from members where member_id=1")).isEqualTo(before)
        assertThatThrownBy { rc().execute { service.complete(1, "김길동", "SERVER", appleOnly = true) } }
            .isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `다른 값의 동시 완료 요청은 하나만 성공한다`() {
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val results = listOf("홍길동", "김길동").map { name ->
                pool.submit(Callable { start.await(); runCatching { rc().execute { service.complete(1, name, "WEB") } } })
            }
            start.countDown()
            val completed = results.map { it.get(10, TimeUnit.SECONDS) }
            assertThat(completed.count { it.isSuccess }).isEqualTo(1)
            assertThat(completed.single { it.isFailure }.exceptionOrNull()).isInstanceOf(BusinessException::class.java)
            assertThat(profiles.findProfile(1)!!.completedAt).isNotNull()
        } finally { pool.shutdownNow() }
    }

    @Test
    fun `삭제된 회원은 새 프로필을 저장하지 않는다`() {
        jdbc.update("update members set deleted_at=now(6), status='WITHDRAWN' where member_id=1")
        assertThatThrownBy { rc().execute { service.complete(1, "홍길동", "WEB") } }.isInstanceOf(MemberDeletedException::class.java)
        assertThat(profiles.findProfile(1)!!.completedAt).isNull()
    }

    @Test
    fun `SQL은 기존 정상 회원만 완료하고 재실행시 신규 회원은 완료하지 않는다`() {
        jdbc.update("update members set name='홍길동' where member_id=1")
        jdbc.update("insert into members (member_id,name,signup_email,part,status,created_at) values (2,'nickname','invalid@example.com','WEB','ACTIVE',now(6)),(3,'김길동','missing@example.com',null,'PENDING',now(6)),(4,'','blank@example.com','SERVER','ACTIVE',now(6))")
        val before = jdbc.queryForList("select member_id,name,part,status from members order by member_id")
        jdbc.execute("alter table members drop column profile_completed_at")
        migrate()
        assertThat(profiles.findProfile(1)!!.completedAt).isNotNull()
        listOf(2L, 3L, 4L).forEach { assertThat(profiles.findProfile(it)!!.completedAt).isNull() }
        assertThat(jdbc.queryForList("select member_id,name,part,status from members order by member_id")).isEqualTo(before)
        jdbc.update("insert into members (member_id,name,signup_email,part,status,created_at) values (5,'박길동','new@example.com','WEB','PENDING',now(6))")
        migrate()
        assertThat(profiles.findProfile(5)!!.completedAt).isNull()
        assertThat(sentinelCount()).isZero()
    }

    @Test
    fun `SQL은 컬럼 추가와 기본값 변경 직후 중단도 복구한다`() {
        listOf(false, true).forEach { defaultChanged ->
            jdbc.update("update members set name='홍길동',part='WEB' where member_id=1")
            jdbc.execute("alter table members drop column profile_completed_at")
            jdbc.execute("alter table members add column profile_completed_at datetime(6) null default '1970-01-02 00:00:00.000000'")
            if (defaultChanged) jdbc.execute("alter table members alter column profile_completed_at set default null")
            migrate()
            assertThat(profiles.findProfile(1)!!.completedAt).isNotNull()
            assertThat(sentinelCount()).isZero()
        }
    }

    private fun sentinelCount() = jdbc.queryForObject("select count(*) from members where profile_completed_at='1970-01-02 00:00:00.000000'", Long::class.java)!!

    private fun migrate() {
        val relative = "db/pending/2610092100_member_profile_completion.sql"
        val file = listOf(Path.of(relative), Path.of("..", relative)).first { Files.exists(it) }
        // SET/PREPARE/EXECUTE의 세션 변수를 같은 연결에서 유지한다.
        jdbc.execute(org.springframework.jdbc.core.ConnectionCallback { connection ->
            Files.readString(file).lineSequence().filterNot { it.trimStart().startsWith("--") }.joinToString("\n")
                .split(';').map(String::trim).filter(String::isNotEmpty).forEach { sql ->
                    connection.createStatement().use { it.execute(sql) }
                }
        })
    }

    private fun rc() = TransactionTemplate(tx).apply { isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = ["core.entity"])
    @EnableJpaRepositories(basePackages = ["core.persistence"])
    @Import(JooqDslConfig::class, MemberRepository::class, MemberProfileRepository::class)
    class Config

    companion object {
        @JvmStatic @BeforeAll
        fun requireLocal() = AttendanceConcurrencyMySqlIntegrationTest.requireDisposableLocalDatabase()

        @JvmStatic @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) = AttendanceConcurrencyMySqlIntegrationTest.mysqlProperties(registry)
    }
}
