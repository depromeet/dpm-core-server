package core.application.member.application.service

import core.application.common.exception.BusinessException
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.member.enums.MemberActivityStatus
import core.domain.member.port.outbound.MemberActivityPersistencePort
import core.domain.member.port.outbound.MemberCohortActivity
import core.domain.member.port.outbound.MemberPersistencePort
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Instant

class MemberActivityServiceTest {
    private val activities = mock(MemberActivityPersistencePort::class.java)
    private val members = mock(MemberPersistencePort::class.java)
    private val cohorts = mock(CohortQueryUseCase::class.java)
    private val service = MemberActivityService(activities, members, cohorts)

    @AfterEach
    fun clearAuthentication() = SecurityContextHolder.clearContext()

    @Test
    fun `same cohort suspension and end cannot be activated again`() {
        listOf(MemberActivityStatus.SUSPENDED, MemberActivityStatus.ENDED).forEach { status ->
            `when`(activities.find(1, 19)).thenReturn(MemberCohortActivity(1, 19, status))
            assertThatThrownBy { service.activate(1, 19) }.isInstanceOf(BusinessException::class.java)
        }
        verify(activities, never()).transition(1, 19, MemberActivityStatus.ACTIVE, null)
    }

    @Test
    fun `a retained suspension marker prevents accidental restore even with ACTIVE projection`() {
        `when`(activities.find(1, 19)).thenReturn(MemberCohortActivity(1, 19, MemberActivityStatus.ACTIVE, Instant.EPOCH))
        assertThatThrownBy { service.requireRestorable(1, 19) }.isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `new cohort activation records authenticated administrator without rewriting previous cohort`() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("99", null, emptyList())
        service.activate(1, 20)
        verify(activities).transition(1, 20, MemberActivityStatus.ACTIVE, 99)
        verify(activities, never()).find(1, 19)
    }

    @Test
    fun `unknown actor and missing activity fail before changing data`() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("unknown", null, emptyList())
        assertThatThrownBy { service.activate(1, 20) }.isInstanceOf(IllegalStateException::class.java)
        SecurityContextHolder.clearContext()
        assertThatThrownBy { service.change(listOf(1), 19, MemberActivityStatus.SUSPENDED) }.isInstanceOf(BusinessException::class.java)
    }
}
