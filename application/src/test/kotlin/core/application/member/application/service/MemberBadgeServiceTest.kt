package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.application.member.application.exception.MemberExceptionCode
import core.application.member.presentation.response.MemberBadgeCard
import core.application.member.presentation.response.MemberManagementResponse.MemberSummary
import core.domain.attendance.enums.AttendanceGraduationStatus
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberBadgePersistencePort
import core.domain.member.port.outbound.MemberBadgeState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class MemberBadgeServiceTest {
    private val store = Store()
    private val cohorts = mock(CohortQueryUseCase::class.java).also { `when`(it.getLatestCohortId()).thenReturn(CohortId(19)) }
    private val targets = mock(MemberManagementTargetQueryService::class.java)
    private val service = MemberBadgeService(store, cohorts, targets)

    @Test
    fun `처음 기존 대상은 확인 완료이고 조회만으로 새 버전을 지우지 않는다`() {
        source(row(1))
        assertThat(service.getBadges().cards).allMatch { !it.hasNew && it.version == 0L }
        source(row(1), row(2))
        service.reconcile(19, null)
        assertThat(pending().version).isEqualTo(1)
        assertThat(pending().hasNew).isTrue()
        assertThat(pending().hasNew).isTrue()
    }

    @Test
    fun `이탈 재진입과 같은 수 대상 교체를 구분하고 대상이 없어지면 확인된다`() {
        source(row(1), row(2))
        service.getBadges()
        source(row(2))
        service.reconcile(19, null)
        assertThat(pending().version).isZero()
        source(row(1), row(2))
        service.reconcile(19, null)
        assertThat(pending().version).isEqualTo(1)
        service.acknowledge(MemberBadgeCard.PENDING, 19, 1)
        source(row(2), row(3))
        service.reconcile(19, null)
        assertThat(pending().version).isEqualTo(2)
        assertThat(pending().hasNew).isTrue()
        source()
        service.reconcile(19, null)
        assertThat(pending().version).isEqualTo(2)
        assertThat(pending().hasNew).isFalse()
    }

    @Test
    fun `오래된 확인과 반복 확인은 새 진입을 지우지 않고 미래 버전과 다른 기수를 거절한다`() {
        source()
        service.getBadges()
        source(row(1))
        service.reconcile(19, null)
        service.acknowledge(MemberBadgeCard.PENDING, 19, 1)
        source(row(1), row(2))
        service.reconcile(19, null)
        repeat(2) { assertThat(service.acknowledge(MemberBadgeCard.PENDING, 19, 1).hasNew).isTrue() }
        assertThat(assertThrows<BusinessException> { service.acknowledge(MemberBadgeCard.PENDING, 19, 3) }.getCode()).isEqualTo(MemberExceptionCode.INVALID_MEMBER_BADGE_VERSION)
        assertThat(assertThrows<BusinessException> { service.acknowledge(MemberBadgeCard.PENDING, 18, 2) }.getCode()).isEqualTo(MemberExceptionCode.MEMBER_BADGE_COHORT_CHANGED)
        assertThat(service.acknowledge(MemberBadgeCard.PENDING, 19, 2).hasNew).isFalse()
    }

    @Test
    fun `첫 쓰기는 변경 직전 대상을 seed하고 첫 신규 진입도 보존한다`() {
        source(row(1))
        val before = service.snapshot(19)
        source(row(1), row(2))
        service.reconcile(19, before)
        assertThat(pending().version).isEqualTo(1)
        assertThat(pending().hasNew).isTrue()
    }

    @Test
    fun `정보 미입력과 수료 위험 불가 카드는 공통 조회 판정을 사용한다`() {
        source()
        service.getBadges()
        source(row(1, MemberStatus.ACTIVE, missing = true, risk = AttendanceGraduationStatus.AT_RISK), row(2, MemberStatus.INACTIVE, risk = AttendanceGraduationStatus.IMPOSSIBLE))
        service.reconcile(19, null)
        val cards = service.getBadges().cards.associateBy { it.card }
        assertThat(cards.getValue(MemberBadgeCard.INCOMPLETE).version).isEqualTo(1)
        assertThat(cards.getValue(MemberBadgeCard.AT_RISK).version).isEqualTo(2)
        assertThat(cards.getValue(MemberBadgeCard.PENDING).hasNew).isFalse()
    }

    private fun pending() = service.getBadges().cards.single { it.card == MemberBadgeCard.PENDING }

    private fun source(vararg rows: MemberSummary) {
        `when`(targets.findAll(19)).thenReturn(rows.toList())
    }

    private fun row(
        id: Long,
        status: MemberStatus = MemberStatus.PENDING,
        missing: Boolean = false,
        risk: AttendanceGraduationStatus? = null,
    ) = MemberSummary(id, 19, "홍길동", "test@example.com", "SERVER", "DEEPER", 1, status, missing, risk, false, null)

    private class Store : MemberBadgePersistencePort {
        val states = mutableMapOf<Pair<Long, String>, MemberBadgeState>()
        val memberships = mutableMapOf<Pair<Long, String>, Set<Long>>()

        override fun findStates(cohortId: Long) = states.values.filter { it.cohortId == cohortId }

        override fun lockStates(cohortId: Long): List<MemberBadgeState> {
            MemberBadgeCard.entries.forEach { states.putIfAbsent(cohortId to it.name, MemberBadgeState(cohortId, it.name)) }
            return findStates(cohortId)
        }

        override fun findMembers(
            cohortId: Long,
            card: String,
        ) = memberships[cohortId to card].orEmpty()

        override fun replaceMembers(
            cohortId: Long,
            card: String,
            previous: Set<Long>,
            members: Set<Long>,
        ) {
            memberships[cohortId to card] = members
        }

        override fun save(state: MemberBadgeState) {
            states[state.cohortId to state.card] = state
        }
    }
}
