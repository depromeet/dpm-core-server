package core.application.member.application.service

import core.application.member.application.service.cohort.MemberCohortService
import core.application.member.application.service.oauth.MemberOAuthService
import core.application.member.application.service.role.MemberRoleService
import core.application.member.application.service.team.MemberTeamService
import core.application.security.oauth.token.JwtTokenInjector
import core.application.security.properties.SecurityProperties
import core.application.security.properties.TokenProperties
import core.domain.member.aggregate.Member
import core.domain.member.enums.MemberPart
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.MemberId
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import core.domain.refreshToken.port.inbound.RefreshTokenInvalidator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletResponse

class MemberWithdrawalTest {
    @ParameterizedTest
    @CsvSource(".example.com,true", "localhost,false")
    fun `탈퇴는 발급된 로그인 쿠키 두 개를 만료하고 세션과 연결을 정리한다`(
        domain: String,
        secure: Boolean,
    ) {
        val members = stub<MemberPersistencePort>()
        val queries = stub<MemberQueryService>()
        val teams = stub<MemberTeamService>()
        val cohorts = stub<MemberCohortService>()
        val sessions = stub<RefreshTokenInvalidator>()
        val roles = stub<MemberRoleService>()
        val oauth = stub<MemberOAuthService>()
        val credentials = stub<MemberCredentialPersistencePort>()
        val injector =
            JwtTokenInjector(
                TokenProperties("test-key", TokenProperties.ExpirationTime(3600, 7200)),
                SecurityProperties("/logout", SecurityProperties.Cookie(domain, true, secure)),
            )
        val memberId = MemberId(1)
        val member = Member(memberId, "홍길동", "member@example.com", "member@example.com", MemberPart.SERVER, MemberStatus.ACTIVE)
        `when`(queries.getMemberById(memberId)).thenReturn(member)
        `when`(members.save(member)).thenReturn(member)
        val service = MemberCommandService(members, queries, teams, cohorts, injector, sessions, roles, oauth, credentials, stub(), stub(), stub(), stub(), stub())
        val issued = MockHttpServletResponse()
        injector.injectAccessToken("access", issued)
        injector.injectRefreshToken("refresh", issued)
        val response = MockHttpServletResponse()

        service.withdraw(memberId, response)

        listOf("accessToken", "refreshToken").forEach { name ->
            val before = requireNotNull(issued.getCookie(name))
            val after = requireNotNull(response.getCookie(name))
            assertThat(after.value).isEmpty()
            assertThat(after.maxAge).isZero()
            assertThat(after.domain).isEqualTo(before.domain)
            assertThat(after.path).isEqualTo(before.path)
            assertThat(after.secure).isEqualTo(before.secure)
            assertThat(after.isHttpOnly).isTrue()
            assertThat(after.getAttribute("SameSite")).isEqualTo(before.getAttribute("SameSite"))
        }
        verify(sessions).destroyRefreshToken(memberId)
        verify(teams).deleteMemberFromTeam(memberId)
        verify(cohorts).deleteMemberFromCohort(memberId)
        verify(roles).revokeAllRoles(memberId)
        verify(oauth).deleteAllByMemberId(memberId)
        verify(credentials).deleteByMemberId(memberId)
        assertThat(member.status).isEqualTo(MemberStatus.WITHDRAWN)
        assertThat(member.deletedAt).isNotNull()
        assertThat(member.name).isEqualTo("홍길동")
        assertThat(member.part).isEqualTo(MemberPart.SERVER)
    }

    private inline fun <reified T : Any> stub(): T = mock(T::class.java)
}
