package core.it.attendance

import core.application.attendance.application.exception.AbsenceReasonImageAlreadyAttachedException
import core.application.attendance.application.exception.AbsenceReasonTooLongException
import core.application.attendance.application.exception.AttendanceExceptionCode
import core.application.attendance.application.exception.InvalidAbsenceReasonImageException
import core.application.attendance.application.service.AbsenceReasonCommandService
import core.application.attendance.application.service.AbsenceReasonQueryService
import core.application.attendance.application.service.AttendanceCommandService
import core.application.attendance.presentation.response.AbsenceReasonImageInfo
import core.application.common.exception.BusinessException
import core.application.image.FakeImageStoragePort
import core.application.member.application.service.access.MemberAccessService
import core.application.support.MutableClock
import core.domain.absencereason.port.inbound.command.AbsenceReasonReviewCommand
import core.domain.absencereason.port.inbound.command.AbsenceReportCreateCommand
import core.domain.absencereason.port.inbound.command.AbsenceReportUpdateCommand
import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
import core.domain.attendance.port.inbound.command.AttendanceRecordCommand
import core.domain.attendance.port.inbound.command.AttendanceStatusUpdateCommand
import core.domain.attendance.port.outbound.AttendancePersistencePort
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.image.aggregate.Image
import core.domain.image.enums.ImageContentType
import core.domain.image.port.outbound.ImagePersistencePort
import core.domain.image.vo.ImageId
import core.domain.member.aggregate.Member
import core.domain.member.enums.MemberStatus
import core.domain.member.port.inbound.MemberQueryUseCase
import core.domain.member.port.outbound.MemberRolePersistencePort
import core.domain.member.vo.MemberId
import core.domain.notification.event.AbsenceReasonReviewedEvent
import core.domain.notification.event.AbsenceReasonSubmittedEvent
import core.domain.notification.port.outbound.NotificationPersistencePort
import core.domain.session.aggregate.Session
import core.domain.session.port.outbound.SessionPersistencePort
import core.domain.session.vo.AttendancePolicy
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

/** 결석 사유서 첨부의 원자적 교체·UNIQUE 동시성·운영진 조회 범위를 MySQL 에서 검증한다. 실행 조건은 [AttendanceConcurrencyMySqlIntegrationTest] 와 같다. */
@Tag("mysql-integration")
@EnabledIfEnvironmentVariable(named = AttendanceConcurrencyMySqlIntegrationTest.URL_ENV, matches = ".+")
@SpringBootTest(
    classes = [AbsenceReasonImageMySqlIntegrationTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
class AbsenceReasonImageMySqlIntegrationTest {
    @Autowired lateinit var commandService: AbsenceReasonCommandService

    @Autowired lateinit var queryService: AbsenceReasonQueryService

    @Autowired lateinit var attendanceCommand: AttendanceCommandService

    @Autowired lateinit var memberRoles: MemberRolePersistencePort

    @Autowired lateinit var notifications: NotificationPersistencePort

    @Autowired lateinit var events: CommittedAbsenceReasonEvents

    @Autowired lateinit var transactionManager: PlatformTransactionManager

    @Autowired lateinit var storage: FakeImageStoragePort

    @Autowired lateinit var cohortPort: CohortPersistencePort

    @Autowired lateinit var sessionPort: SessionPersistencePort

    @Autowired lateinit var attendancePort: AttendancePersistencePort

    @Autowired lateinit var imagePort: ImagePersistencePort

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @Autowired lateinit var clock: MutableClock

    @MockitoBean lateinit var memberQueryUseCase: MemberQueryUseCase

    @MockitoBean lateinit var memberAccessService: MemberAccessService

    @Test
    fun `생략은 기존 첨부 유지, 빈 목록은 해제, 값은 순서대로 교체하며 같은 이미지로 순서만 바꿀 수 있다`() {
        val member = newMember()
        val session = newSession()
        val (a, b) = List(2) { newImage(member) }

        submit(session, member, "사유", imageIds = null)
        assertThat(myImageIds(session, member)).isEmpty()

        submit(session, member, "사유", listOf(a, b))
        assertThat(myImageIds(session, member)).containsExactly(a.value, b.value)

        update(session, member, "기존 클라이언트 수정", imageIds = null)
        submit(session, member, "기존 클라이언트 재제출", imageIds = null)
        assertThat(myImageIds(session, member)).containsExactly(a.value, b.value)

        update(session, member, "순서 변경", listOf(b, a))
        assertThat(myImageIds(session, member)).containsExactly(b.value, a.value)

        update(session, member, "해제", emptyList())
        assertThat(myImageIds(session, member)).isEmpty()
        assertThat(queryService.getMyAbsenceReason(session.id!!, member)!!.contents).isEqualTo("해제")
    }

    @Test
    fun `남의 이미지, 없는 이미지, 잘못된 id, 중복, 50자 초과는 400 이고 아무것도 바꾸지 않는다`() {
        val member = newMember()
        val session = newSession()
        val mine = newImage(member)
        val others = newImage(newMember())
        submit(session, member, "원래 사유", listOf(mine))

        listOf(listOf(mine, others), listOf(mine, ImageId(Long.MAX_VALUE)), listOf(ImageId(0))).forEach { imageIds ->
            assertThatThrownBy { update(session, member, "바뀜", imageIds) }
                .isInstanceOf(InvalidAbsenceReasonImageException::class.java)
        }
        assertThatThrownBy { update(session, member, "바뀜", listOf(mine, mine)) }
            .isInstanceOf(InvalidAbsenceReasonImageException::class.java)
            .matches { (it as BusinessException).getCode() == AttendanceExceptionCode.DUPLICATE_ABSENCE_REASON_IMAGE }
        assertThatThrownBy { update(session, member, "가".repeat(51), null) }
            .isInstanceOf(AbsenceReasonTooLongException::class.java)

        val reason = queryService.getMyAbsenceReason(session.id!!, member)!!
        assertThat(reason.contents).isEqualTo("원래 사유")
        assertThat(reason.imageIds).containsExactly(mine.value)
    }

    @Test
    fun `다른 사유서에 붙은 이미지는 409 이고 사유서 저장까지 롤백되며 해제나 삭제 후에는 다시 붙일 수 있다`() {
        val member = newMember()
        val (first, second) = List(2) { newSession() }
        val image = newImage(member)
        submit(first, member, "첫 사유", listOf(image))
        submit(second, member, "둘째 사유", emptyList())

        // 사유서 내용을 저장한 뒤 첨부 단계에서 거절되므로 내용 변경도 롤백돼야 한다
        assertThatThrownBy { update(second, member, "바뀌면 안 됨", listOf(image)) }
            .isInstanceOf(AbsenceReasonImageAlreadyAttachedException::class.java)
        assertThat(queryService.getMyAbsenceReason(second.id!!, member)!!.contents).isEqualTo("둘째 사유")
        assertThat(myImageIds(second, member)).isEmpty()
        assertThatThrownBy { submit(newSession(), member, "새 사유", listOf(image)) }
            .isInstanceOf(AbsenceReasonImageAlreadyAttachedException::class.java)

        update(first, member, "해제", emptyList())
        update(second, member, "재사용", listOf(image))
        assertThat(myImageIds(second, member)).containsExactly(image.value)

        commandService.deleteAbsenceReason(second.id!!, member)
        assertThat(linkCount(image)).isZero()
        assertThat(imagePort.findById(image)).isNotNull()
        update(first, member, "삭제 후 재사용", listOf(image))
        assertThat(myImageIds(first, member)).containsExactly(image.value)
    }

    @Test
    fun `다른 세션 사유서에 같은 이미지를 동시에 붙이면 하나만 성공하고 나머지는 409 다`() {
        repeat(5) {
            val member = newMember()
            val sessions = List(6) { newSession() }
            val (x, y) = List(2) { newImage(member) }

            val results =
                runConcurrently(sessions.size) { index ->
                    // 겹치는 이미지를 서로 다른 순서로 넣어도 교착 없이 UNIQUE 로 끝나야 한다
                    val imageIds = if (index % 2 == 0) listOf(x, y) else listOf(y, x)
                    submit(sessions[index], member, "동시 $index", imageIds)
                }

            assertThat(results.count { it.isSuccess }).isEqualTo(1)
            assertThat(results.mapNotNull { it.exceptionOrNull() })
                .allMatch { it is AbsenceReasonImageAlreadyAttachedException }
            assertThat(linkCount(x)).isEqualTo(1)
            assertThat(linkCount(y)).isEqualTo(1)
            // 실패한 요청의 새 사유서는 남지 않는다
            assertThat(reasonCount(member)).isEqualTo(1)
        }
    }

    @Test
    fun `같은 세션 동시 제출은 세션 잠금으로 사유서 하나에 마지막 첨부만 남긴다`() {
        val member = newMember()
        val session = newSession()
        val images = List(4) { newImage(member) }

        val results =
            runConcurrently(4) { index ->
                submit(session, member, "동시 $index", listOf(images[index], images[(index + 1) % 4]))
            }

        assertThat(results).allMatch { it.isSuccess }
        assertThat(reasonCount(member)).isEqualTo(1)
        assertThat(myImageIds(session, member)).hasSize(2)
    }

    @Test
    fun `승인은 기존 출석 경로로 인정결석을 남기고 첨부는 유지한다`() {
        val member = newMember()
        val session = newSession()
        val image = newImage(member)
        attendancePort.save(Attendance(sessionId = session.id!!, memberId = member, status = AttendanceStatus.ABSENT))
        submit(session, member, "사유", listOf(image))

        commandService.reviewAbsenceReason(AbsenceReasonReviewCommand(session.id!!, member, approved = true))

        assertThat(attendancePort.findAttendanceBy(session.id!!.value, member.value)!!.status)
            .isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
        assertThat(myImageIds(session, member)).containsExactly(image.value)
    }

    @Test
    fun `제출은 출석을 그대로 두고 출석 행이 없으면 승인을 롤백한다`() {
        val session = newSession()
        AttendanceStatus.entries.forEach { status ->
            val member = newMember()
            attendancePort.save(Attendance(sessionId = session.id!!, memberId = member, status = status))
            submit(session, member, "사유", null)
            assertThat(attendanceStatus(session, member)).isEqualTo(status)
            assertThat(queryService.getMyAbsenceReason(session.id!!, member)!!.status).isEqualTo("PENDING")
            assertThat(queryService.getSessionAbsenceReasons(session.id!!).reasons.single { it.memberId == member.value }.status)
                .isEqualTo("PENDING")
            commandService.deleteAbsenceReason(session.id!!, member)
            assertThat(attendanceStatus(session, member)).isEqualTo(status)
        }
        val noRow = newMember()
        submit(session, noRow, "사유", null)
        assertThatThrownBy { review(session, noRow, true) }.isInstanceOf(BusinessException::class.java)
        assertThat(reasonStatus(session, noRow)).isEqualTo("PENDING")
    }

    @Test
    fun `반려와 수정은 지각을 보존하고 승인 후에는 변경할 수 없다`() {
        val session = newSession()
        val member = newMember()
        attendancePort.save(Attendance(sessionId = session.id!!, memberId = member, status = AttendanceStatus.LATE))
        submit(session, member, "첫 사유", null)
        review(session, member, false)
        review(session, member, false)
        assertThat(attendanceStatus(session, member)).isEqualTo(AttendanceStatus.LATE)
        assertThat(reasonStatus(session, member)).isEqualTo("REJECTED")
        update(session, member, "보완", null)
        assertThat(reasonStatus(session, member)).isEqualTo("PENDING")
        assertThat(attendanceStatus(session, member)).isEqualTo(AttendanceStatus.LATE)
        review(session, member, true)
        review(session, member, true)
        listOf<() -> Unit>(
            { update(session, member, "수정", null) },
            { submit(session, member, "재제출", null) },
            { commandService.deleteAbsenceReason(session.id!!, member) },
            { review(session, member, false) },
        ).forEach { action -> assertThatThrownBy { action() }.isInstanceOf(BusinessException::class.java) }
        assertThat(queryService.getMyAbsenceReason(session.id!!, member)!!.contents).isEqualTo("보완")
        assertThat(reasonStatus(session, member)).isEqualTo("APPROVED")
        assertThat(attendanceStatus(session, member)).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
    }

    /** 승인 상태가 다시 같아져도 예전 버전으로 번복할 수 없고, 커밋된 결정만 알림을 만든다. */
    @Test
    fun `같은 심사 버전의 동시 정정은 한 번만 적용되고 승인 반려가 왕복해도 이전 요청은 거절한다`() {
        val session = newSession()
        val member = newMember()
        val sessionId = requireNotNull(session.id)
        attendancePort.save(Attendance(sessionId = sessionId, memberId = member, status = AttendanceStatus.ABSENT))
        submit(session, member, "사유", null)
        review(session, member, true)
        val previous = reviewVersion(session, member)
        val correction = AbsenceReasonReviewCommand(sessionId, member, false, previous, AttendanceStatus.ABSENT)

        val results = runConcurrently(2) { commandService.reviewAbsenceReason(correction) }
        assertThat(results.count { it.isSuccess }).isEqualTo(1)
        assertThat((results.single { it.isFailure }.exceptionOrNull() as BusinessException).getCode())
            .isEqualTo(AttendanceExceptionCode.ABSENCE_REASON_REVIEW_VERSION_CONFLICT)
        assertThat(reasonStatus(session, member)).isEqualTo("REJECTED")
        assertThat(attendanceStatus(session, member)).isEqualTo(AttendanceStatus.ABSENT)

        commandService.reviewAbsenceReason(AbsenceReasonReviewCommand(sessionId, member, true, reviewVersion(session, member)))
        assertThatThrownBy { commandService.reviewAbsenceReason(correction) }
            .matches { it is BusinessException && it.getCode() == AttendanceExceptionCode.ABSENCE_REASON_REVIEW_VERSION_CONFLICT }
        assertThat(reasonStatus(session, member)).isEqualTo("APPROVED")
        assertThat(attendanceStatus(session, member)).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
        assertThat(events.reviews.filter { it.memberId == member }.map { it.approved }).containsExactly(true, false, true)
    }

    /** 상태 저장뿐 아니라 실제 코드 입력을 확인하고, 거절된 요청이 기존 승인과 출석을 보존하는지 검사한다. */
    @Test
    fun `승인 취소로 인증을 다시 열면 코드 인증이 가능하고 당일 19시에는 정정 전체가 거절된다`() {
        val session = newSession(Instant.parse("2026-10-10T04:00:00Z"))
        val (openMember, closedMember) = List(2) { newMember() }
        val sessionId = requireNotNull(session.id)
        val previousClock = clock.instant()
        try {
            listOf(openMember, closedMember).forEach { member ->
                attendancePort.save(Attendance(sessionId = sessionId, memberId = member, status = AttendanceStatus.ABSENT))
                submit(session, member, "사유", null)
                review(session, member, true)
            }
            val beforeDeadline = session.lateParticipationDeadline().minusSeconds(1)
            clock.now = beforeDeadline
            val reopen =
                AbsenceReasonReviewCommand(sessionId, openMember, false, reviewVersion(session, openMember), AttendanceStatus.PENDING)
            assertThatThrownBy { commandService.reviewAbsenceReason(reopen) }
                .matches { it is BusinessException && it.getCode() == AttendanceExceptionCode.ATTENDANCE_REOPEN_CLOSED }
            assertThat(reasonStatus(session, openMember)).isEqualTo("APPROVED")
            assertThat(attendanceStatus(session, openMember)).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)

            jdbcTemplate.update("update cohorts set is_active = true where cohort_id = ?", session.cohortId.value)
            commandService.reviewAbsenceReason(reopen)
            assertThat(reasonStatus(session, openMember)).isEqualTo("REJECTED")
            assertThat(attendanceRow(session, openMember)).isEqualTo(Triple("PENDING", false, false))
            val recorded = attendanceCommand.attendSession(AttendanceRecordCommand(sessionId, openMember, beforeDeadline, "4321"))
            assertThat(recorded.status).isEqualTo(AttendanceStatus.ABSENT)
            assertThat(attendancePort.findAttendanceBy(sessionId.value, openMember.value)?.lateParticipatedAt).isEqualTo(beforeDeadline)

            clock.now = session.lateParticipationDeadline()
            val closedCorrection =
                AbsenceReasonReviewCommand(
                    sessionId,
                    closedMember,
                    false,
                    reviewVersion(session, closedMember),
                    AttendanceStatus.PENDING,
                )
            assertThatThrownBy { commandService.reviewAbsenceReason(closedCorrection) }
                .matches { it is BusinessException && it.getCode() == AttendanceExceptionCode.ATTENDANCE_REOPEN_CLOSED }
            assertThat(reasonStatus(session, closedMember)).isEqualTo("APPROVED")
            assertThat(attendanceStatus(session, closedMember)).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
            assertThat(events.reviews.filter { it.memberId == closedMember }.map { it.approved }).containsExactly(true)
        } finally {
            jdbcTemplate.update("update cohorts set is_active = false where cohort_id = ?", session.cohortId.value)
            clock.now = previousClock
        }
    }

    /** 일괄 요청에서 충돌 대상 앞에 정상 대상을 넣어, 일부만 먼저 저장되는 경로도 확인한다. */
    @Test
    fun `승인된 사유서와 충돌하는 단건과 일괄 변경은 다른 대상도 수정하지 않는다`() {
        val session = newSession()
        val (reviewed, other) = List(2) { newMember() }
        val sessionId = requireNotNull(session.id)
        listOf(reviewed, other).forEach { member ->
            attendancePort.save(Attendance(sessionId = sessionId, memberId = member, status = AttendanceStatus.PENDING))
        }
        submit(session, reviewed, "사유", null)
        review(session, reviewed, true)

        assertThatThrownBy {
            attendanceCommand.updateAttendanceStatus(AttendanceStatusUpdateCommand(sessionId, reviewed, AttendanceStatus.PENDING))
        }.matches { it is BusinessException && it.getCode() == AttendanceExceptionCode.APPROVED_ABSENCE_REASON_ATTENDANCE_CONFLICT }
        assertThatThrownBy {
            attendanceCommand.updateAttendanceStatusBulk(sessionId, AttendanceStatus.ABSENT, listOf(other, reviewed))
        }.matches { it is BusinessException && it.getCode() == AttendanceExceptionCode.APPROVED_ABSENCE_REASON_ATTENDANCE_CONFLICT }
        assertThat(reasonStatus(session, reviewed)).isEqualTo("APPROVED")
        assertThat(attendanceStatus(session, reviewed)).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
        assertThat(attendanceStatus(session, other)).isEqualTo(AttendanceStatus.PENDING)
    }

    private fun reviewVersion(
        session: Session,
        member: MemberId,
    ): String = queryService.getSessionAbsenceReasons(requireNotNull(session.id)).reasons.single { it.memberId == member.value }.reviewVersion

    @Test
    fun `승인과 마감 배치가 동시에 실행돼도 인정결석이 남고 검토 중 미출석은 자동 결석이 된다`() {
        val session = newSession()
        val (reviewed, pending) = List(2) { newMember() }
        listOf(reviewed, pending).forEach {
            attendancePort.save(Attendance(sessionId = session.id!!, memberId = it, status = AttendanceStatus.PENDING))
            submit(session, it, "사유", null)
        }
        val results =
            runConcurrently(3) { index ->
                if (index == 0) {
                    attendanceCommand.closeExpiredAttendances(session.id!!, session.attendancePolicy.absentStart)
                } else {
                    review(session, reviewed, true)
                }
            }
        assertThat(results).allMatch { it.isSuccess }
        assertThat(reasonStatus(session, reviewed)).isEqualTo("APPROVED")
        assertThat(attendanceStatus(session, reviewed)).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
        assertThat(reasonStatus(session, pending)).isEqualTo("PENDING")
        assertThat(attendanceRow(session, pending)).isEqualTo(Triple("ABSENT", false, true))
        review(session, pending, true)
        assertThat(attendanceStatus(session, pending)).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
        assertThat(attendanceCommand.closeExpiredAttendances(session.id!!, session.attendancePolicy.absentStart)).isZero()
    }

    @Test
    fun `승인과 반려가 동시에 도착하면 한 결정만 반영된다`() {
        val session = newSession()
        val member = newMember()
        attendancePort.save(Attendance(sessionId = session.id!!, memberId = member, status = AttendanceStatus.ABSENT))
        submit(session, member, "사유", null)
        val results = runConcurrently(2) { index -> review(session, member, index == 0) }
        assertThat(results.count { it.isSuccess }).isEqualTo(1)
        assertThat(results.single { it.isFailure }.exceptionOrNull()).isInstanceOf(BusinessException::class.java)
        if (results.first().isSuccess) {
            assertThat(reasonStatus(session, member)).isEqualTo("APPROVED")
            assertThat(attendanceStatus(session, member)).isEqualTo(AttendanceStatus.EXCUSED_ABSENT)
        } else {
            assertThat(reasonStatus(session, member)).isEqualTo("REJECTED")
            assertThat(attendanceStatus(session, member)).isEqualTo(AttendanceStatus.ABSENT)
        }
    }

    @Test
    fun `검토 대기 진입과 심사 결과마다 커밋된 알림 이벤트가 한 번만 발생한다`() {
        val session = newSession()
        val member = newMember()
        attendancePort.save(Attendance(sessionId = session.id!!, memberId = member, status = AttendanceStatus.ABSENT))
        submit(session, member, "사유", null)
        submit(session, member, "사유", null)
        update(session, member, "편집", null)
        review(session, member, false)
        review(session, member, false)
        update(session, member, "보완", null)
        submit(session, member, "보완", null)
        TransactionTemplate(transactionManager).executeWithoutResult { tx ->
            review(session, member, true)
            tx.setRollbackOnly()
        }
        assertThat(reasonStatus(session, member)).isEqualTo("PENDING")
        assertThat(events.reviews.filter { it.memberId == member }.map { it.approved }).containsExactly(false)
        assertThat(runConcurrently(2) { review(session, member, true) }).allMatch { it.isSuccess }
        assertThat(events.submissions.filter { it.memberId == member }.map { it.sessionId })
            .containsExactly(session.id, session.id)
        assertThat(events.reviews.filter { it.memberId == member }.map { it.approved }).containsExactly(false, true)
    }

    @Test
    fun `같은 기수의 활성 운영진과 코어만 수신하고 비대상자와 공유하는 기기는 제외한다`() {
        val cohort = newSession().cohortId
        val otherCohort = newSession().cohortId
        val (organizer, core, graduate, deeper, inactive) = List(5) { newMember() }
        val legacyOrganizer = newMember()
        val suspendedCore = newMember()
        jdbcTemplate.update("update cohorts set is_active = true where cohort_id = ?", cohort.value)
        val staffRole = 901
        val coreRole = 902
        jdbcTemplate.update("insert into roles (role_id, name) values (?, 'ORGANIZER'), (?, 'CORE')", staffRole, coreRole)
        listOf(organizer, core, graduate, deeper, inactive, legacyOrganizer, suspendedCore).forEach { member ->
            jdbcTemplate.update(
                "insert into members (member_id, name, signup_email, part, status, created_at) values (?, '디퍼', ?, 'SERVER', ?, now(6))",
                member.value,
                "${member.value}@example.test",
                if (member == inactive) "INACTIVE" else "ACTIVE",
            )
            jdbcTemplate.update(
                "insert into member_cohorts (member_id, cohort_id) values (?, ?)",
                member.value,
                if (member == graduate) otherCohort.value else cohort.value,
            )
        }
        listOf(organizer to staffRole, organizer to coreRole, core to coreRole, graduate to staffRole, inactive to coreRole)
            .forEach { (member, role) ->
                jdbcTemplate.update(
                    "insert into member_roles (member_id, role_id, cohort_id, granted_at) values (?, ?, ?, now(6))",
                    member.value,
                    role,
                    if (member == graduate) otherCohort.value else cohort.value,
                )
            }
        listOf(legacyOrganizer to staffRole, suspendedCore to coreRole, graduate to coreRole).forEach { (member, role) ->
            jdbcTemplate.update(
                "insert into member_roles (member_id, role_id, cohort_id, granted_at) values (?, ?, null, now(6))",
                member.value,
                role,
            )
        }
        jdbcTemplate.update(
            """
            insert into member_cohort_activities (member_id, cohort_id, status, created_at, updated_at)
            select member_id, cohort_id, if(member_id = ?, 'SUSPENDED', 'ACTIVE'), now(6), now(6)
            from member_cohorts where cohort_id in (?, ?)
            """.trimIndent(),
            suspendedCore.value,
            cohort.value,
            otherCohort.value,
        )
        val recipients = memberRoles.findAttendanceStaffIds(cohort)
        assertThat(recipients).containsExactlyInAnyOrder(organizer, core, legacyOrganizer)

        listOf(organizer to "staff", core to "staff", core to "core-only", core to "mixed", deeper to "mixed").forEach { (member, token) ->
            jdbcTemplate.update("insert into notification_tokens (member_id, token, created_at) values (?, ?, now(6))", member.value, token)
        }
        assertThat(notifications.findTokensForRecipients(recipients)).containsExactlyInAnyOrder("staff", "core-only")
        assertThat(notifications.findTokensForRecipients(listOf(core))).containsExactly("core-only")
    }

    private fun review(
        session: Session,
        member: MemberId,
        approved: Boolean,
    ) = commandService.reviewAbsenceReason(AbsenceReasonReviewCommand(session.id!!, member, approved))

    @Test
    fun `운영진 목록은 사유서별 imageIds 와 파일명을 순서대로, 없으면 빈 목록으로 준다`() {
        val session = newSession()
        val (withImages, withoutImages) = List(2) { newMember() }
        val a = newImage(withImages, fileName = "진단서.jpg")
        val b = newImage(withImages)
        submit(session, withImages, "첨부", listOf(b, a))
        submit(session, withoutImages, "첨부 없음", null)

        val items = queryService.getSessionAbsenceReasons(session.id!!).reasons.associateBy { it.memberId }

        assertThat(items.getValue(withImages.value).imageIds).containsExactly(b.value, a.value)
        assertThat(items.getValue(withImages.value).images)
            .containsExactly(AbsenceReasonImageInfo(b.value, null), AbsenceReasonImageInfo(a.value, "진단서.jpg"))
        assertThat(items.getValue(withoutImages.value).imageIds).isEmpty()
        assertThat(items.getValue(withoutImages.value).images).isEmpty()
    }

    @Test
    fun `내 사유서는 첨부 이미지 파일명을 순서대로 준다`() {
        val session = newSession()
        val member = newMember()
        val a = newImage(member, fileName = "진단서.jpg")
        val b = newImage(member)
        submit(session, member, "첨부", listOf(a, b))

        assertThat(queryService.getMyAbsenceReason(session.id!!, member)!!.images)
            .containsExactly(AbsenceReasonImageInfo(a.value, "진단서.jpg"), AbsenceReasonImageInfo(b.value, null))
    }

    private fun submit(
        session: Session,
        memberId: MemberId,
        contents: String,
        imageIds: List<ImageId>?,
    ) = commandService.submitAbsenceReason(AbsenceReportCreateCommand(session.id!!, memberId, contents, imageIds))

    private fun update(
        session: Session,
        memberId: MemberId,
        contents: String,
        imageIds: List<ImageId>?,
    ) = commandService.updateAbsenceReason(AbsenceReportUpdateCommand(session.id!!, memberId, contents, imageIds))

    private fun attendanceStatus(
        session: Session,
        memberId: MemberId,
    ): AttendanceStatus = attendancePort.findAttendanceBy(session.id!!.value, memberId.value)!!.status

    /** status, 운영진 변경 표시(updated_at) 유무, 자동 결석 표지(auto_absent_at) 유무 */
    private fun attendanceRow(
        session: Session,
        memberId: MemberId,
    ): Triple<String, Boolean, Boolean> =
        jdbcTemplate.queryForObject(
            "select status, updated_at, auto_absent_at from attendances where session_id = ? and member_id = ? and deleted_at is null",
            { rs, _ -> Triple(rs.getString(1), rs.getTimestamp(2) != null, rs.getTimestamp(3) != null) },
            session.id!!.value,
            memberId.value,
        )!!

    private fun reasonStatus(
        session: Session,
        memberId: MemberId,
    ): String =
        jdbcTemplate.queryForObject(
            "select status from absence_reasons where session_id = ? and member_id = ?",
            String::class.java,
            session.id!!.value,
            memberId.value,
        )!!

    private fun myImageIds(
        session: Session,
        memberId: MemberId,
    ): List<Long> = queryService.getMyAbsenceReason(session.id!!, memberId)!!.imageIds

    /** 사유서 행만 있으면 되므로 회원 행은 만들지 않고 제출 알림에 쓰는 조회만 흉내 낸다. */
    private fun newMember(): MemberId {
        val memberId = MemberId(ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_000_000_000L))
        given(memberQueryUseCase.getMemberById(memberId))
            .willReturn(Member(id = memberId, name = "디퍼", signupEmail = "${memberId.value}@it.test", status = MemberStatus.ACTIVE))
        return memberId
    }

    private fun newImage(
        owner: MemberId,
        fileName: String? = null,
    ): ImageId {
        val bytes = UUID.randomUUID().toString().toByteArray()
        // 이미지 행은 업로드 완료로만 만들어지고 entity 모듈은 이 모듈 컴파일 경로에 없으므로 행을 직접 넣는다.
        val objectKey = Image.OBJECT_KEY_PREFIX + UUID.randomUUID()
        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({ connection ->
            connection
                .prepareStatement(
                    "insert into images (owner_member_id, object_key, content_type, size_bytes, created_at, original_file_name) " +
                        "values (?, ?, ?, ?, ?, ?)",
                    arrayOf("image_id"),
                ).apply {
                    setLong(1, owner.value)
                    setString(2, objectKey)
                    setString(3, ImageContentType.PNG.mimeType)
                    setLong(4, bytes.size.toLong())
                    setTimestamp(5, Timestamp.from(Instant.now()))
                    setString(6, fileName)
                }
        }, keyHolder)
        storage.put(objectKey, bytes)
        return ImageId(keyHolder.key!!.toLong())
    }

    private fun newSession(start: Instant = Instant.parse("2026-10-10T10:00:00Z")): Session {
        val cohortId = cohortPort.save(Cohort(value = "it583-" + UUID.randomUUID().toString().substring(0, 8))).id!!
        return sessionPort.save(
            Session(
                cohortId = cohortId,
                date = start,
                week = 1,
                place = "온라인",
                eventName = "첨부 통합 테스트",
                attendancePolicy =
                    AttendancePolicy(
                        attendanceStart = start,
                        lateStart = start.plus(Duration.ofMinutes(10)),
                        absentStart = start.plus(Duration.ofMinutes(30)),
                        attendanceCode = "4321",
                    ),
            ),
        )
    }

    private fun reasonCount(memberId: MemberId): Int = jdbcTemplate.queryForObject("select count(*) from absence_reasons where member_id = ?", Int::class.javaObjectType, memberId.value)!!

    private fun linkCount(imageId: ImageId): Int = jdbcTemplate.queryForObject("select count(*) from absence_reason_images where image_id = ?", Int::class.javaObjectType, imageId.value)!!

    private fun <T> runConcurrently(
        count: Int,
        task: (Int) -> T,
    ): List<Result<T>> {
        val ready = CountDownLatch(count)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(count)
        try {
            val futures =
                (0 until count).map { index ->
                    pool.submit(
                        Callable {
                            ready.countDown()
                            start.await()
                            runCatching { task(index) }
                        },
                    )
                }
            ready.await()
            start.countDown()
            return futures.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun requireDisposableLocalDatabase() = AttendanceConcurrencyMySqlIntegrationTest.requireDisposableLocalDatabase()

        @JvmStatic
        @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) = AttendanceConcurrencyMySqlIntegrationTest.mysqlProperties(registry)
    }
}

/** 실제 트랜잭션 경계에서 전달된 이벤트만 관찰한다. */
class CommittedAbsenceReasonEvents {
    val submissions = CopyOnWriteArrayList<AbsenceReasonSubmittedEvent>()
    val reviews = CopyOnWriteArrayList<AbsenceReasonReviewedEvent>()

    @TransactionalEventListener
    fun onSubmitted(event: AbsenceReasonSubmittedEvent) {
        submissions.add(event)
    }

    @TransactionalEventListener
    fun onReviewed(event: AbsenceReasonReviewedEvent) {
        reviews.add(event)
    }
}
