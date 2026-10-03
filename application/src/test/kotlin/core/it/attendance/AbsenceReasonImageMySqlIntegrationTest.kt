package core.it.attendance

import core.application.attendance.application.exception.AbsenceReasonImageAlreadyAttachedException
import core.application.attendance.application.exception.AbsenceReasonTooLongException
import core.application.attendance.application.exception.AttendanceExceptionCode
import core.application.attendance.application.exception.InvalidAbsenceReasonImageException
import core.application.attendance.application.service.AbsenceReasonCommandService
import core.application.attendance.application.service.AbsenceReasonImageQueryService
import core.application.attendance.application.service.AbsenceReasonQueryService
import core.application.common.exception.BusinessException
import core.application.image.FakeImageStoragePort
import core.application.image.application.exception.ImageNotFoundException
import core.application.image.application.service.ImageQueryService
import core.domain.absencereason.port.inbound.command.AbsenceReasonReviewCommand
import core.domain.absencereason.port.inbound.command.AbsenceReportCreateCommand
import core.domain.absencereason.port.inbound.command.AbsenceReportUpdateCommand
import core.domain.attendance.aggregate.Attendance
import core.domain.attendance.enums.AttendanceStatus
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
import core.domain.member.vo.MemberId
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
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
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

    @Autowired lateinit var imageQueryService: AbsenceReasonImageQueryService

    @Autowired lateinit var generalImageQueryService: ImageQueryService

    @Autowired lateinit var storage: FakeImageStoragePort

    @Autowired lateinit var cohortPort: CohortPersistencePort

    @Autowired lateinit var sessionPort: SessionPersistencePort

    @Autowired lateinit var attendancePort: AttendancePersistencePort

    @Autowired lateinit var imagePort: ImagePersistencePort

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @MockitoBean lateinit var memberQueryUseCase: MemberQueryUseCase

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
    fun `운영진 목록은 사유서별 imageIds 를 순서대로, 없으면 빈 목록으로 준다`() {
        val session = newSession()
        val (withImages, withoutImages) = List(2) { newMember() }
        val (a, b) = List(2) { newImage(withImages) }
        submit(session, withImages, "첨부", listOf(b, a))
        submit(session, withoutImages, "첨부 없음", null)

        val items = queryService.getSessionAbsenceReasons(session.id!!).reasons.associateBy { it.memberId }

        assertThat(items.getValue(withImages.value).imageIds).containsExactly(b.value, a.value)
        assertThat(items.getValue(withoutImages.value).imageIds).isEmpty()
    }

    @Test
    fun `운영진 원본 조회는 그 사유서에 붙은 그 멤버 이미지만 허용하고 거부할 때는 저장소를 부르지 않는다`() {
        val member = newMember()
        val admin = newMember()
        val (session, otherSession) = List(2) { newSession() }
        val (attached, unattached, attachedElsewhere) = List(3) { newImage(member) }
        val strangers = newImage(newMember())
        submit(session, member, "사유", listOf(attached))
        submit(otherSession, member, "다른 사유", listOf(attachedElsewhere))
        // 서비스로는 만들 수 없는 '남의 이미지 링크'도 소유자 확인에서 막히는지 본다
        jdbcTemplate.update(
            "insert into absence_reason_images (absence_reason_id, image_id, display_order) values (?, ?, 9)",
            reasonId(session, member),
            strangers.value,
        )

        storage.calls.clear()
        val content = imageQueryService.getAbsenceReasonImage(session.id!!, member, attached)
        assertThat(content.bytes).isEqualTo(storage.objects.getValue(imagePort.findById(attached)!!.objectKey))
        assertThat(storage.calls).containsExactly("get")

        storage.calls.clear()
        listOf(
            Triple(session, member, unattached),
            Triple(session, member, attachedElsewhere),
            Triple(session, member, strangers),
            Triple(session, admin, attached),
            Triple(otherSession, member, attached),
        ).forEach { (s, m, image) ->
            assertThatThrownBy { imageQueryService.getAbsenceReasonImage(s.id!!, m, image) }
                .isInstanceOf(ImageNotFoundException::class.java)
        }
        // 일반 조회는 운영진에게도 소유자 전용이다
        assertThatThrownBy { generalImageQueryService.getImage(admin, attached) }
            .isInstanceOf(ImageNotFoundException::class.java)

        update(session, member, "해제", emptyList())
        assertThatThrownBy { imageQueryService.getAbsenceReasonImage(session.id!!, member, attached) }
            .isInstanceOf(ImageNotFoundException::class.java)
        commandService.deleteAbsenceReason(otherSession.id!!, member)
        assertThatThrownBy { imageQueryService.getAbsenceReasonImage(otherSession.id!!, member, attachedElsewhere) }
            .isInstanceOf(ImageNotFoundException::class.java)
        assertThat(storage.calls).isEmpty()
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

    private fun newImage(owner: MemberId): ImageId {
        val bytes = UUID.randomUUID().toString().toByteArray()
        val image = imagePort.save(Image.create(owner, ImageContentType.PNG, bytes.size.toLong(), Instant.now()))
        storage.objects[image.objectKey] = bytes
        return image.id!!
    }

    private fun newSession(): Session {
        val cohortId = cohortPort.save(Cohort(value = "it583-" + UUID.randomUUID().toString().substring(0, 8))).id!!
        val start = Instant.parse("2026-10-10T10:00:00Z")
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

    private fun reasonId(
        session: Session,
        memberId: MemberId,
    ): Long =
        jdbcTemplate.queryForObject(
            "select absence_reason_id from absence_reasons where session_id = ? and member_id = ?",
            Long::class.javaObjectType,
            session.id!!.value,
            memberId.value,
        )!!

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
