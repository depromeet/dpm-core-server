package core.it.member

import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.member.vo.MemberId
import core.domain.notification.port.inbound.SentSessionNotificationCommandUseCase
import core.it.attendance.AttendanceConcurrencyMySqlIntegrationTest
import core.it.attendance.AttendanceMySqlIntegrationTestApplication
import core.persistence.member.repository.MemberRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = AttendanceConcurrencyMySqlIntegrationTest.URL_ENV, matches = ".+")
@SpringBootTest(classes = [AttendanceMySqlIntegrationTestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(MemberRepository::class)
class MemberPurgeMySqlIntegrationTest {
    @Autowired lateinit var jdbc: JdbcTemplate

    @Autowired lateinit var members: MemberRepository

    @Autowired lateinit var transactionManager: PlatformTransactionManager

    @MockitoBean lateinit var memberQueries: core.domain.member.port.inbound.MemberQueryUseCase

    @MockitoBean lateinit var feedbackForms: core.application.sessionFeedback.application.service.SessionFeedbackFormCommandService

    @MockitoBean lateinit var roles: RoleQueryUseCase

    @MockitoBean lateinit var notifications: SentSessionNotificationCommandUseCase

    @BeforeEach
    fun fixture() {
        (MEMBER_TABLES.keys + CHILD_TABLES + listOf("members", "teams", "roles", "cohorts")).forEach { jdbc.execute("delete from $it") }
        jdbc.update("insert into cohorts (cohort_id, value, is_active, created_at, updated_at) values (19, '19', true, 0, 0)")
        jdbc.update("insert into teams (team_id, number, cohort_id, created_at, updated_at) values (191, 1, 19, 0, 0)")
        jdbc.update("insert into roles (role_id, name) values (1, 'DEEPER')")
        // 1번은 삭제 대상(소프트 삭제 상태), 2번은 보존 대상. 두 회원 모두 같은 데이터를 갖는다.
        (1L..2L).forEach { id ->
            val other = 3 - id
            jdbc.update("insert into members (member_id, name, signup_email, email, part, status, created_at, updated_at, deleted_at) values (?, '홍길동', ?, ?, 'SERVER', 'ACTIVE', '2020-01-01', '2020-01-02', ?)", id, "signup$id@example.com", "email$id@example.com", if (id == 1L) "2026-10-01" else null)
            jdbc.update("insert into member_cohorts (member_id, cohort_id) values (?, 19)", id)
            jdbc.update("insert into member_roles (member_id, role_id, cohort_id, granted_at) values (?, 1, 19, now(6))", id)
            jdbc.update("insert into member_permissions (member_id, permission_id, granted_at) values (?, 1, now(6))", id)
            jdbc.update("insert into member_teams (member_id, team_id) values (?, 191)", id)
            jdbc.update("insert into member_oauth (member_id, external_id, provider, email) values (?, ?, 'KAKAO', 'oauth@example.com')", id, "10$id")
            jdbc.update("insert into member_credentials (member_id, email, password, created_at) values (?, ?, 'test-hash', now(6))", id, "signup$id@example.com")
            jdbc.update("insert into refresh_tokens (member_id, token_hash, issued_at, expires_at) values (?, ?, now(6), '2099-01-01')", id, "hash$id")
            jdbc.update("insert into notification_tokens (member_id, token, created_at) values (?, ?, now(6))", id, "fcm$id")
            jdbc.update("insert into member_admission_events (member_id, event_type, created_at) values (?, 'APPROVED', now(6))", id)
            jdbc.update("insert into attendances (session_id, member_id, status, attended_at) values (1, ?, 'PRESENT', now(6))", id)
            // 불참 사유와 첨부 이미지, 직접 업로드 기록
            jdbc.update("insert into images (image_id, owner_member_id, object_key, content_type, size_bytes, created_at) values (?, ?, ?, 'image/png', 1, now(6))", id, id, "images/$id")
            jdbc.update("insert into image_uploads (upload_id, owner_member_id, content_type, size_bytes, status, expires_at, image_id, created_at) values (?, ?, 'image/png', 1, 'COMPLETED', now(6), ?, now(6))", "upload-$id", id, id)
            jdbc.update("insert into absence_reasons (absence_reason_id, session_id, member_id, contents, status, created_at) values (?, 1, ?, '사유', 'PENDING', now(6))", id, id)
            jdbc.update("insert into absence_reason_images (absence_reason_id, image_id, display_order) values (?, ?, 0)", id, id)
            // 세션 피드백과 선택 항목
            jdbc.update("insert into session_feedbacks (session_feedback_id, session_id, member_id, satisfaction, submitted_at) values (?, 1, ?, 'GOOD', now(6))", id, id)
            jdbc.update("insert into session_feedback_aspects (session_feedback_id, question, aspect) values (?, 'LIKED', 'CONTENT')", id)
            // 본인이 작성한 공지와 과제, 다른 회원의 읽음·제출
            jdbc.update("insert into announcements (announcement_id, announcement_type, title, content, author_id, cohort_id, created_at, updated_at) values (?, 0, '공지', '본문', ?, 19, now(6), now(6))", id, id)
            jdbc.update("insert into announcement_reads (announcement_id, member_id, read_at) values (?, ?, now(6)), (?, ?, now(6))", id, id, id, other)
            jdbc.update("insert into assignments (assignment_id, submit_type, created_at, updated_at) values (?, 0, now(6), now(6))", id)
            jdbc.update("insert into announcement_assignments (announcement_id, assignment_id) values (?, ?)", id, id)
            jdbc.update("insert into assignment_submissions (assignment_id, member_id, team_id, submit_type, submit_status, score, created_at, updated_at) values (?, ?, 191, 0, 0, 10, now(6), now(6))", id, id)
            // 본인이 만든 회식·정산·모임과 다른 회원의 참여
            jdbc.update("insert into after_party (after_party_id, title, category, scheduled_at, closed_at, is_approved, member_id, created_at, updated_at, can_edit_after_approval) values (?, '회식', 'AFTER_PARTY', '2099-01-01', '2099-01-01', false, ?, now(6), now(6), false)", id, id)
            jdbc.update("insert into after_party_invitees (after_party_id, member_id, rsvp_status, is_attended, invited_at) values (?, ?, true, true, now(6)), (?, ?, true, true, now(6))", id, id, id, other)
            jdbc.update("insert into bill_accounts (bill_account_id, bill_account_value, account_holder_name, bank_name, account_type, created_at, updated_at) values (?, 'test-account', '홍길동', 'test-bank', 'ACCOUNT', now(6), now(6))", id)
            jdbc.update("insert into bills (bill_id, bill_account_id, title, description, bill_status, host_user_id, created_at, updated_at) values (?, ?, '정산', '설명', 'OPEN', ?, now(6), now(6))", id, id, id)
            jdbc.update("insert into gatherings (gathering_id, bill_id, title, held_at, category, host_user_id, round_number, created_at, updated_at) values (?, ?, '모임', now(6), 'GATHERING', ?, 1, now(6), now(6))", id, id, id)
            jdbc.update("insert into gathering_members (gathering_id, member_id, is_viewed, is_joined, is_invitation_submitted, created_at, updated_at) values (?, ?, true, true, true, now(6), now(6)), (?, ?, true, true, true, now(6), now(6))", id, id, id, other)
        }
    }

    @Test
    fun `회원과 회원을 참조하는 모든 행을 지우고 다른 회원 데이터는 남긴다`() {
        val keys = TransactionTemplate(transactionManager).execute { members.hardDeleteById(MemberId(1)) }

        assertThat(keys).containsExactly("images/1")
        assertThat(count("members", "member_id", 1)).isZero()
        MEMBER_TABLES.forEach { (table, column) ->
            assertThat(count(table, column, 1)).`as`(table).isZero()
            assertThat(count(table, column, 2)).`as`(table).isPositive()
        }
        // 1번 소유 부모(공지·회식·정산·모임·불참 사유·피드백)에 매달린 다른 회원 행과 하위 행도 사라진다.
        assertThat(count("announcement_reads", "announcement_id", 1)).isZero()
        assertThat(count("after_party_invitees", "after_party_id", 1)).isZero()
        assertThat(count("gathering_members", "gathering_id", 1)).isZero()
        assertThat(count("announcement_assignments", "announcement_id", 1)).isZero()
        assertThat(count("assignments", "assignment_id", 1)).isZero()
        assertThat(count("absence_reason_images", "absence_reason_id", 1)).isZero()
        assertThat(count("session_feedback_aspects", "session_feedback_id", 1)).isZero()
        assertThat(count("absence_reason_images", "absence_reason_id", 2)).isOne()
        assertThat(count("session_feedback_aspects", "session_feedback_id", 2)).isOne()
        assertThat(count("assignments", "assignment_id", 2)).isOne()
    }

    private fun count(
        table: String,
        column: String,
        value: Long,
    ) = jdbc.queryForObject("select count(*) from $table where $column = ?", Long::class.java, value)!!

    companion object {
        /** 회원 id를 직접 참조하는 테이블과 컬럼. */
        private val MEMBER_TABLES =
            linkedMapOf(
                "member_cohorts" to "member_id", "member_roles" to "member_id", "member_permissions" to "member_id",
                "member_teams" to "member_id", "member_oauth" to "member_id", "member_credentials" to "member_id",
                "refresh_tokens" to "member_id", "notification_tokens" to "member_id", "member_admission_events" to "member_id",
                "attendances" to "member_id", "images" to "owner_member_id", "image_uploads" to "owner_member_id",
                "absence_reasons" to "member_id", "session_feedbacks" to "member_id", "announcements" to "author_id",
                "announcement_reads" to "member_id", "assignment_submissions" to "member_id", "after_party" to "member_id",
                "after_party_invitees" to "member_id", "bills" to "host_user_id", "gatherings" to "host_user_id",
                "gathering_members" to "member_id",
            )
        private val CHILD_TABLES =
            listOf("absence_reason_images", "session_feedback_aspects", "announcement_assignments", "assignments", "bill_accounts", "after_party_invite_tags")

        @JvmStatic @BeforeAll
        fun requireDisposableLocalDatabase() = AttendanceConcurrencyMySqlIntegrationTest.requireDisposableLocalDatabase()

        @JvmStatic @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) = AttendanceConcurrencyMySqlIntegrationTest.mysqlProperties(registry)
    }
}
