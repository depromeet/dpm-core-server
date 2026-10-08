package core.it.member

import com.fasterxml.jackson.databind.ObjectMapper
import core.application.member.application.exception.MemberNotFoundException
import core.application.member.application.service.MemberDeletionService
import core.application.member.application.service.MemberLoginService
import core.application.member.application.service.MemberManagementCommandService
import core.application.member.application.service.oauth.MemberOAuthService
import core.application.member.application.service.role.MemberRoleService
import core.application.member.application.service.team.MemberTeamService
import core.application.member.presentation.request.MemberManagementUpdateRequest
import core.application.refreshToken.application.exception.TokenInvalidException
import core.application.refreshToken.application.service.RefreshTokenIssueService
import core.application.refreshToken.application.service.RefreshTokenService
import core.application.security.oauth.token.DeviceIdResolver
import core.application.security.oauth.token.JwtAuthenticationFilter
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.oauth.token.JwtTokenProvider
import core.application.security.oauth.token.JwtTokenResolver
import core.application.security.properties.TokenProperties
import core.domain.announcement.vo.AnnouncementId
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.bill.vo.BillId
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import core.domain.notification.port.inbound.SentSessionNotificationCommandUseCase
import core.domain.refreshToken.aggregate.RefreshToken
import core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort
import core.domain.security.oauth.dto.KakaoAuthAttributes
import core.it.attendance.AttendanceConcurrencyMySqlIntegrationTest
import core.it.attendance.AttendanceMySqlIntegrationTestApplication
import core.persistence.announcement.repository.AnnouncementRepository
import core.persistence.bill.repository.BillRepository
import core.persistence.gathering.repository.GatheringRepository
import core.persistence.member.repository.MemberRepository
import core.persistence.member.repository.role.MemberRoleRepository
import core.persistence.member.repository.team.MemberTeamRepository
import core.persistence.refreshToken.repository.RefreshTokenRepository
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = AttendanceConcurrencyMySqlIntegrationTest.URL_ENV, matches = ".+")
@SpringBootTest(classes = [AttendanceMySqlIntegrationTestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(RefreshTokenRepository::class, MemberRepository::class, MemberDeletionService::class, MemberManagementCommandService::class, MemberRoleRepository::class, MemberTeamRepository::class, AnnouncementRepository::class, BillRepository::class, GatheringRepository::class)
class MemberDeletionMySqlIntegrationTest {
    @Autowired lateinit var jdbc: JdbcTemplate

    @Autowired lateinit var members: MemberPersistencePort

    @Autowired lateinit var tokenStore: RefreshTokenPersistencePort

    @Autowired lateinit var deletion: MemberDeletionService

    @Autowired lateinit var management: MemberManagementCommandService

    @Autowired lateinit var announcements: AnnouncementRepository

    @Autowired lateinit var bills: BillRepository

    @Autowired lateinit var gatherings: GatheringRepository

    @Autowired lateinit var transactionManager: PlatformTransactionManager

    @MockitoBean lateinit var memberQueries: core.domain.member.port.inbound.MemberQueryUseCase

    @MockitoBean lateinit var feedbackForms: core.application.sessionFeedback.application.service.SessionFeedbackFormCommandService

    @MockitoBean lateinit var roles: RoleQueryUseCase

    @MockitoBean lateinit var notifications: SentSessionNotificationCommandUseCase

    @BeforeEach
    fun fixture() {
        (RELATED_TABLES + listOf("members", "teams", "roles", "cohorts")).forEach { jdbc.execute("delete from $it") }
        jdbc.update("insert into cohorts (cohort_id, value, is_active, created_at, updated_at) values (19, '19', true, 0, 0)")
        (1L..3L).forEach { id ->
            jdbc.update("insert into members (member_id, name, signup_email, email, part, status, created_at, updated_at) values (?, '홍길동', ?, ?, 'SERVER', 'ACTIVE', '2020-01-01', '2020-01-02')", id, "signup$id@example.com", "email$id@example.com")
            jdbc.update("insert into member_cohorts (member_id, cohort_id) values (?, 19)", id)
        }
        jdbc.update("insert into teams (team_id, number, cohort_id, created_at, updated_at) values (191, 1, 19, 0, 0)")
        jdbc.update("insert into roles (role_id, name) values (1, 'DEEPER')")
        jdbc.update("insert into member_roles (member_id, role_id, cohort_id, granted_at) values (1, 1, 19, now(6))")
        jdbc.update("insert into member_teams (member_id, team_id) values (1, 191)")
        jdbc.update("insert into member_oauth (member_id, external_id, provider, email) values (1, '101', 'KAKAO', 'oauth@example.com')")
        jdbc.update("insert into member_credentials (member_id, email, password, created_at) values (1, 'signup1@example.com', 'test-hash', now(6))")
        jdbc.update("insert into refresh_tokens (member_id, token_hash, issued_at, expires_at) values (1, 'test-hash', now(6), '2099-01-01')")
        jdbc.update("insert into attendances (session_id, member_id, status, attended_at) values (1, 1, 'PRESENT', now(6))")
        jdbc.update("insert into announcements (announcement_id, announcement_type, title, content, author_id, created_at, updated_at) values (1, 0, '공지', '보존할 본문', 1, now(6), now(6))")
        jdbc.update("insert into announcement_reads (announcement_id, member_id, read_at) values (1, 1, now(6)), (1, 2, now(6))")
        jdbc.update("insert into assignments (assignment_id, submit_type, created_at, updated_at) values (1, 0, now(6), now(6))")
        jdbc.update("insert into assignment_submissions (assignment_id, member_id, team_id, submit_type, submit_status, score, created_at, updated_at) values (1, 1, 191, 0, 0, 10, now(6), now(6))")
        jdbc.update("insert into after_party (after_party_id, title, category, scheduled_at, closed_at, is_approved, member_id, created_at, updated_at, can_edit_after_approval) values (1, '회식', 'AFTER_PARTY', '2099-01-01', '2099-01-01', false, 1, now(6), now(6), false)")
        jdbc.update("insert into after_party_invitees (after_party_id, member_id, rsvp_status, is_attended, invited_at) values (1, 1, true, true, now(6)), (1, 2, true, true, now(6))")
        jdbc.update("insert into bill_accounts (bill_account_id, bill_account_value, account_holder_name, bank_name, account_type, created_at, updated_at) values (1, 'test-account', '홍길동', 'test-bank', 'ACCOUNT', now(6), now(6))")
        jdbc.update("insert into bills (bill_id, bill_account_id, title, description, bill_status, host_user_id, created_at, updated_at) values (1, 1, '정산', '보존', 'OPEN', 1, now(6), now(6))")
        jdbc.update("insert into gatherings (gathering_id, bill_id, title, held_at, category, host_user_id, round_number, created_at, updated_at) values (1, 1, '모임', now(6), 'GATHERING', 1, 1, now(6), now(6))")
        jdbc.update("insert into gathering_members (gathering_id, member_id, is_viewed, is_joined, is_invitation_submitted, created_at, updated_at) values (1, 1, true, true, true, now(6), now(6)), (1, 2, true, true, true, now(6), now(6))")
    }

    @Test
    fun `회원 두 필드만 바꾸고 모든 관련 데이터와 다른 참여자의 공유 기록 조회를 보존한다`() {
        val before = snapshot()
        val memberBefore = memberRow(1)
        deletion.delete(listOf(1))
        assertThat(snapshot()).isEqualTo(before)
        val after = memberRow(1)
        assertThat(after["status"]).isEqualTo("WITHDRAWN")
        assertThat(after["deleted_at"]).isNotNull()
        assertThat(after.filterKeys { it !in setOf("status", "deleted_at") }).isEqualTo(memberBefore.filterKeys { it !in setOf("status", "deleted_at") })
        assertThat(members.findManagementMembers(19).map { it.memberId }).containsExactlyInAnyOrder(2, 3)
        assertThat(members.findAllByCohortId(CohortId(19))).containsExactlyInAnyOrder(MemberId(2), MemberId(3))
        assertThat(members.isLoginAvailable(1)).isFalse()
        assertThat(members.isLoginAvailable(2)).isTrue()
        TransactionTemplate(transactionManager).executeWithoutResult {
            assertThat(members.findById(MemberId(1))!!.name).isEqualTo("홍길동")
            assertThat(announcements.findAnnouncementById(AnnouncementId(1))!!.content).isEqualTo("보존할 본문")
            assertThat(announcements.findAnnouncementListItems()).hasSize(1)
            assertThat(bills.findById(BillId(1))!!.hostUserId).isEqualTo(MemberId(1))
            assertThat(gatherings.findByBillId(BillId(1)).single().hostUserId).isEqualTo(MemberId(1))
            assertThat(gatherings.getSubmittedParticipantEachGathering(BillId(1), MemberId(2))).hasSize(1)
        }
        assertThatThrownBy { deletion.delete(listOf(1)) }.isInstanceOf(MemberNotFoundException::class.java)
        assertThat(memberRow(1)).isEqualTo(after)
    }

    @Test
    fun `없음 삭제 탈퇴 대상을 섞으면 전체 취소하고 DB 실패도 앞선 변경을 롤백한다`() {
        val before = memberRow(1)
        assertThatThrownBy { deletion.delete(listOf(1, 999)) }.isInstanceOf(MemberNotFoundException::class.java)
        jdbc.update("update members set status = 'WITHDRAWN' where member_id = 2")
        assertThatThrownBy { deletion.delete(listOf(1, 2)) }.isInstanceOf(MemberNotFoundException::class.java)
        assertThat(memberRow(1)).isEqualTo(before)
        jdbc.update("update members set status = 'ACTIVE' where member_id = 2")
        jdbc.execute("create trigger member623_reject_delete before update on members for each row begin if new.member_id = 2 and new.deleted_at is not null then signal sqlstate '45000' set message_text = 'rollback test'; end if; end")
        try {
            assertThatThrownBy { deletion.delete(listOf(2, 1)) }.isInstanceOf(RuntimeException::class.java)
        } finally {
            jdbc.execute("drop trigger member623_reject_delete")
        }
        assertThat(memberRow(1)).isEqualTo(before)
        assertThat(memberRow(2)["deleted_at"]).isNull()
        deletion.delete(listOf(1, 2))
        assertThat(memberRow(1)["deleted_at"]).isNotNull()
        assertThat(memberRow(2)["deleted_at"]).isNotNull()
    }

    @Test
    fun `삭제를 기다린 관리 수정은 최신 삭제상태를 읽고 복원하지 않는다`() {
        val changed = CountDownLatch(1)
        val release = CountDownLatch(1)
        val connectionId = AtomicLong()
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first =
                pool.submit(
                    Callable {
                        TransactionTemplate(transactionManager).executeWithoutResult {
                            deletion.delete(listOf(1))
                            connectionId.set(jdbc.queryForObject("select connection_id()", Long::class.java)!!)
                            changed.countDown()
                            check(release.await(10, TimeUnit.SECONDS))
                        }
                    },
                )
            check(changed.await(10, TimeUnit.SECONDS))
            val second = pool.submit(Callable { runCatching { management.update(1, MemberManagementUpdateRequest(status = "ACTIVE")) } })
            awaitLock(connectionId.get())
            release.countDown()
            first.get(10, TimeUnit.SECONDS)
            assertThat(second.get(10, TimeUnit.SECONDS).exceptionOrNull()).isInstanceOf(core.application.member.application.exception.MemberManagementTargetNotAllowedException::class.java)
            assertThat(memberRow(1)["status"]).isEqualTo("WITHDRAWN")
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    @Test
    fun `삭제 직전 발급된 JWT와 기존 OAuth는 삭제 직후 접근 재발급 로그인을 모두 차단한다`() {
        val provider = JwtTokenProvider(TokenProperties("dGVzdC1zZWNyZXQta2V5LWZvci1kZWxldGlvbi10ZXN0LXNlY3JldA==", TokenProperties.ExpirationTime(7200, 2592000)), roles)
        val access = provider.generateAccessToken("1", null)
        val refresh = provider.generateRefreshToken("1", null)
        val filter = JwtAuthenticationFilter(provider, JwtTokenResolver(), ObjectMapper(), members)
        deletion.delete(listOf(1))
        listOf("accessToken" to access, "refreshToken" to refresh).forEach { (name, token) ->
            val response = MockHttpServletResponse()
            val chain = MockFilterChain()
            filter.doFilter(MockHttpServletRequest("GET", "/v3/members").apply { setCookies(Cookie(name, token)) }, response, chain)
            assertThat(response.status).isEqualTo(401)
            assertThat(chain.request).isNull()
            assertThat(SecurityContextHolder.getContext().authentication).isNull()
        }
        val tokenStore = mock(RefreshTokenPersistencePort::class.java)
        val issuer = mock(RefreshTokenIssueService::class.java)
        val hash = core.application.refreshToken.application.support.TokenHasher.sha256Hex(refresh)
        `when`(tokenStore.findByTokenHash(hash)).thenReturn(RefreshToken(1, MemberId(1), hash, null, Instant.now(), Instant.now().plusSeconds(3600)))
        val refreshService = RefreshTokenService(tokenStore, issuer, JwtTokenResolver(), mock(JwtTokenInjector::class.java), provider, mock(DeviceIdResolver::class.java), members)
        assertThatThrownBy { TransactionTemplate(transactionManager).executeWithoutResult { refreshService.reissue(MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", refresh)) }, MockHttpServletResponse()) } }.isInstanceOf(TokenInvalidException::class.java)
        verifyNoInteractions(issuer)
        val oauth = mock(MemberOAuthService::class.java)
        val roleService = mock(MemberRoleService::class.java)
        val teamService = mock(MemberTeamService::class.java)
        val attributes = KakaoAuthAttributes("101", "changed@example.com", "김철수", OAuthProvider.KAKAO)
        `when`(oauth.findByProviderAndExternalId(OAuthProvider.KAKAO, "101")).thenReturn(MemberOAuth(MemberOAuthId(1), "101", OAuthProvider.KAKAO, MemberId(1), "oauth@example.com"))
        val identityLock = core.application.member.application.service.MemberIdentityLockService(members, mock(core.domain.member.port.outbound.MemberMergePersistencePort::class.java))
        val login = MemberLoginService(members, identityLock, oauth, issuer, roleService, teamService)
        TransactionTemplate(transactionManager).executeWithoutResult {
            assertThatThrownBy { login.handleLoginSuccess(attributes, null) }.isInstanceOf(core.application.member.application.exception.MemberDeletedException::class.java)
        }
        verify(oauth).findByProviderAndExternalId(OAuthProvider.KAKAO, "101")
        verifyNoMoreInteractions(oauth)
        verifyNoInteractions(issuer, roleService, teamService)
        assertThat(jdbc.queryForObject("select email from member_oauth where member_id = 1", String::class.java)).isEqualTo("oauth@example.com")
    }

    @Test
    fun `삭제와 동시 재발급은 회원 잠금 뒤 최신 상태를 보고 토큰을 보존한다`() {
        val provider = JwtTokenProvider(TokenProperties("dGVzdC1zZWNyZXQta2V5LWZvci1kZWxldGlvbi10ZXN0LXNlY3JldA==", TokenProperties.ExpirationTime(7200, 2592000)), roles)
        val refresh = provider.generateRefreshToken("1", null)
        val hash = core.application.refreshToken.application.support.TokenHasher.sha256Hex(refresh)
        jdbc.update("update refresh_tokens set token_hash = ? where member_id = 1", hash)
        val issuer = mock(RefreshTokenIssueService::class.java)
        val service = RefreshTokenService(tokenStore, issuer, JwtTokenResolver(), mock(JwtTokenInjector::class.java), provider, mock(DeviceIdResolver::class.java), members)
        val changed = CountDownLatch(1)
        val release = CountDownLatch(1)
        val connectionId = AtomicLong()
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first =
                pool.submit(
                    Callable {
                        TransactionTemplate(transactionManager).executeWithoutResult {
                            deletion.delete(listOf(1))
                            connectionId.set(jdbc.queryForObject("select connection_id()", Long::class.java)!!)
                            changed.countDown()
                            check(release.await(10, TimeUnit.SECONDS))
                        }
                    },
                )
            check(changed.await(10, TimeUnit.SECONDS))
            val second =
                pool.submit(
                    Callable {
                        runCatching {
                            TransactionTemplate(transactionManager).executeWithoutResult {
                                service.reissue(MockHttpServletRequest().apply { setCookies(Cookie("refreshToken", refresh)) }, MockHttpServletResponse())
                            }
                        }
                    },
                )
            awaitLock(connectionId.get())
            release.countDown()
            first.get(10, TimeUnit.SECONDS)
            assertThat(second.get(10, TimeUnit.SECONDS).exceptionOrNull()).isInstanceOf(TokenInvalidException::class.java)
            verifyNoInteractions(issuer)
            assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where token_hash = ? and rotated_at is null", Int::class.java, hash)).isEqualTo(1)
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    @Test
    fun `회원 잠금 후 refresh 잠금 조회는 이전 JPA 캐시의 회전시각을 재사용하지 않는다`() {
        TransactionTemplate(transactionManager).executeWithoutResult {
            val before = tokenStore.findByTokenHash("test-hash")!!
            assertThat(before.rotatedAt).isNull()
            // 같은 트랜잭션의 직접 갱신으로 JPA 1차 캐시를 의도적으로 낡게 만든다.
            jdbc.update("update refresh_tokens set rotated_at = '2026-01-01' where token_hash = 'test-hash'")
            members.lockApprovalTargets(listOf(1))
            assertThat(tokenStore.lockByTokenHash("test-hash")!!.rotatedAt).isNotNull()
        }
    }

    private fun memberRow(id: Long): Map<String, Any> = jdbc.queryForMap("select * from members where member_id = ?", id)

    private fun snapshot() = RELATED_TABLES.associateWith { jdbc.queryForList("select * from $it").map { row -> row.mapValues { it.value?.toString() } } }

    private fun awaitLock(connectionId: Long) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (System.nanoTime() < deadline) {
            if (jdbc.queryForObject("select count(*) from performance_schema.data_lock_waits w join performance_schema.threads t on t.thread_id = w.blocking_thread_id where t.processlist_id = ?", Long::class.java, connectionId)!! > 0) return
            CountDownLatch(1).await(20, TimeUnit.MILLISECONDS)
        }
        error("관리 수정이 회원 삭제 잠금을 기다리지 않았습니다")
    }

    companion object {
        private val RELATED_TABLES = listOf("gathering_members", "gatherings", "bills", "bill_accounts", "after_party_invitees", "after_party_invite_tags", "after_party", "assignment_submissions", "announcement_reads", "announcement_assignments", "announcements", "assignments", "attendances", "member_credentials", "refresh_tokens", "member_teams", "member_roles", "member_cohorts", "member_permissions", "member_oauth")

        @JvmStatic @BeforeAll
        fun requireDisposableLocalDatabase() = AttendanceConcurrencyMySqlIntegrationTest.requireDisposableLocalDatabase()

        @JvmStatic @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) = AttendanceConcurrencyMySqlIntegrationTest.mysqlProperties(registry)
    }
}
