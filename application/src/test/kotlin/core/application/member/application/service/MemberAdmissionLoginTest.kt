package core.application.member.application.service

import core.application.authorization.application.service.RoleQueryService
import core.application.member.application.exception.MemberAllowedException
import core.application.member.application.exception.MemberDeletedException
import core.application.member.application.service.auth.EmailPasswordAuthService
import core.application.member.application.service.cohort.MemberCohortService
import core.application.member.application.service.role.MemberRoleService
import core.application.member.application.service.team.MemberTeamService
import core.application.refreshToken.application.service.RefreshTokenIssueService
import core.application.security.oauth.token.JwtTokenProvider
import core.domain.cohort.port.inbound.CohortQueryUseCase
import core.domain.cohort.vo.CohortId
import core.domain.member.aggregate.Member
import core.domain.member.enums.LoginMethod
import core.domain.member.enums.MemberStatus
import core.domain.member.port.outbound.MemberAdmissionEventPersistencePort
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.port.outbound.query.MemberApprovalTarget
import core.domain.member.vo.LoginIdentity
import core.domain.member.vo.MemberId
import core.domain.membercredential.aggregate.MemberCredential
import core.domain.membercredential.aggregate.MemberCredentialId
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import core.domain.refreshToken.aggregate.RefreshToken
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant

class MemberAdmissionLoginTest {
    private val credentials = mock(MemberCredentialPersistencePort::class.java)
    private val members = mock(MemberPersistencePort::class.java)
    private val roles = mock(RoleQueryService::class.java)
    private val tokens = mock(JwtTokenProvider::class.java)
    private val refreshTokens = mock(RefreshTokenIssueService::class.java)
    private val passwords = mock(PasswordEncoder::class.java)
    private val events = mock(MemberAdmissionEventPersistencePort::class.java)
    private val service =
        EmailPasswordAuthService(
            credentials,
            members,
            mock(MemberIdentityLockService::class.java),
            roles,
            mock(MemberRoleService::class.java),
            mock(MemberTeamService::class.java),
            tokens,
            refreshTokens,
            passwords,
        )

    @Test
    fun `반려 로그인은 상태를 유지하고 명시 재신청 후에도 이메일 재로그인이 가능하다`() {
        val member = fixture(MemberStatus.REJECTED)
        val cohorts = mock(CohortQueryUseCase::class.java)
        `when`(cohorts.getActiveCohortId()).thenReturn(CohortId(20))
        val admission = MemberAdmissionService(members, events, cohorts, mock(MemberCohortService::class.java))
        `when`(members.lockApprovalTargets(listOf(1))).thenAnswer { listOf(MemberApprovalTarget(1, member.status, emptySet())) }
        doAnswer {
            member.updateStatus(MemberStatus.PENDING)
            null
        }
            .`when`(members).updateManagementFields(listOf(1), false, null, MemberStatus.PENDING, emptySet())

        assertThat(service.login("member@example.com", "password").accessToken).isEqualTo("access")
        assertThat(member.status).isEqualTo(MemberStatus.REJECTED)
        verifyNoInteractions(events)
        admission.reapply(1)
        assertThat(member.status).isEqualTo(MemberStatus.PENDING)
        assertThat(service.login("member@example.com", "password").accessToken).isEqualTo("access")
        assertThat(member.status).isEqualTo(MemberStatus.PENDING)
    }

    @Test
    fun `반려나 대기라도 삭제 회원은 로그인할 수 없고 탈퇴도 그대로 거절한다`() {
        fixture(MemberStatus.REJECTED)
        `when`(members.existsDeletedMemberById(1)).thenReturn(true)
        assertThatThrownBy { service.login("member@example.com", "password") }.isInstanceOf(MemberDeletedException::class.java)
        fixture(MemberStatus.WITHDRAWN)
        assertThatThrownBy { service.login("member@example.com", "password") }.isInstanceOf(MemberAllowedException::class.java)
    }

    @Test
    fun `기존 소셜 회원은 상태와 관계없이 비밀번호 없이 이메일로 연결할 수 없다`() {
        listOf(MemberStatus.PENDING, MemberStatus.REJECTED, MemberStatus.ACTIVE, MemberStatus.INACTIVE).forEach { status ->
            val member = Member(id = MemberId(1), name = "홍길동", signupEmail = "social@example.com", status = status)
            `when`(members.findAllBySignupEmail("social@example.com")).thenReturn(listOf(member))
            assertThatThrownBy { service.login("social@example.com", "attacker-password") }
                .isInstanceOf(core.application.member.application.exception.InvalidEmailPasswordException::class.java)
        }
        verifyNoInteractions(passwords, tokens, refreshTokens)
        assertThat(org.mockito.Mockito.mockingDetails(credentials).invocations).noneMatch { it.method.name == "save" }
    }

    private fun fixture(status: MemberStatus): Member {
        val member = Member(id = MemberId(1), name = "홍길동", email = "member@example.com", signupEmail = "member@example.com", status = status)
        val identity = LoginIdentity(LoginMethod.EMAIL, 1)
        `when`(credentials.findByEmail("member@example.com")).thenReturn(MemberCredential(MemberCredentialId(1), MemberId(1), "member@example.com", "encoded"))
        `when`(passwords.matches("password", "encoded")).thenReturn(true)
        `when`(members.findById(MemberId(1))).thenReturn(member)
        `when`(roles.getPermissionsByMemberId(MemberId(1))).thenReturn(emptyList())
        `when`(tokens.generateAccessTokenWithPermissions("1", emptyList(), identity)).thenReturn("access")
        `when`(refreshTokens.issueForLogin(MemberId(1), null, identity)).thenReturn(
            RefreshToken(memberId = MemberId(1), tokenHash = "hash", plainToken = "refresh", issuedAt = Instant.now(), expiresAt = Instant.now().plusSeconds(600)),
        )
        return member
    }
}
