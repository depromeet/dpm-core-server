package core.application.cohort.application.service

import core.application.cohort.application.exception.CohortNotFoundException
import core.domain.cohort.aggregate.Cohort
import core.domain.cohort.port.outbound.CohortPersistencePort
import core.domain.cohort.vo.CohortId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class CohortQueryServiceTest {
    private val store = mock(CohortPersistencePort::class.java)
    private val service = CohortQueryService(store)

    @Test
    fun `기수가 없으면 nullable 조회는 예외 없이 null이고 기존 필수 조회는 예외이다`() {
        `when`(store.findAll()).thenReturn(emptyList())
        assertThat(service.findCurrentCohortOrNull()).isNull()
        assertThrows<CohortNotFoundException> { service.getLatestCohortId() }
        `when`(store.findAll()).thenReturn(listOf(Cohort(id = CohortId(1), value = "준비")))
        assertThat(service.findCurrentCohortOrNull()).isNull()
    }

    @Test
    fun `활성 기수를 우선하고 없으면 가장 큰 숫자 기수를 고르는 기존 규칙을 공유한다`() {
        val current = Cohort(id = CohortId(19), value = "19")
        val prepared = Cohort(id = CohortId(20), value = "20")
        `when`(store.findAll()).thenReturn(listOf(current, prepared))
        `when`(store.findActive()).thenReturn(current)
        assertThat(service.findCurrentCohortOrNull()?.id).isEqualTo(CohortId(19))
        assertThat(service.getLatestCohortId()).isEqualTo(CohortId(19))
        `when`(store.findActive()).thenReturn(null)
        assertThat(service.findCurrentCohortOrNull()?.id).isEqualTo(CohortId(20))
        assertThat(service.getLatestCohortId()).isEqualTo(CohortId(20))
    }
}
