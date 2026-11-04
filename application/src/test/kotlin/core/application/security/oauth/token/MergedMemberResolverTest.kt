package core.application.security.oauth.token

import core.domain.member.enums.LoginMethod
import core.domain.member.port.outbound.MemberMergePersistencePort
import core.domain.member.port.outbound.MemberMergeTarget
import core.domain.member.port.outbound.MemberPersistencePort
import core.domain.member.vo.LoginIdentity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant

class MergedMemberResolverTest {
    private val tokens = mock(JwtTokenProvider::class.java)
    private val members = mock(MemberPersistencePort::class.java)
    private val merges = mock(MemberMergePersistencePort::class.java)
    private val identities = mock(LoginIdentityValidator::class.java)
    private val resolver = MergedMemberResolver(tokens, members, merges, identities, mock(core.domain.refreshToken.port.outbound.RefreshTokenPersistencePort::class.java))
    private val now = Instant.parse("2026-11-04T01:00:00Z")

    private fun fixture(method: LoginMethod = LoginMethod.KAKAO) {
        `when`(tokens.validateToken("old")).thenReturn(true)
        `when`(tokens.getMemberId("old")).thenReturn(1)
        `when`(tokens.getIssuedAt("old")).thenReturn(now.minusSeconds(10))
        `when`(tokens.getLoginIdentity("old")).thenReturn(LoginIdentity(method, 7))
        `when`(merges.findTarget(1)).thenReturn(MemberMergeTarget(2, now))
        `when`(members.isLoginAvailable(2)).thenReturn(true)
        `when`(identities.isValid(2, LoginIdentity(method, 7))).thenReturn(true)
    }

    @ParameterizedTest
    @EnumSource(value = LoginMethod::class, names = ["KAKAO", "APPLE"])
    fun `either social login resolves to the retained member`(method: LoginMethod) {
        fixture(method)
        assertThat(resolver.resolve("old")).isEqualTo(MergedMemberResolver.ResolvedMember(2, listOf(1, 2)))
    }

    @Test
    fun `ordinary deletion without a merge never redirects the identity`() {
        fixture()
        `when`(merges.findTarget(1)).thenReturn(null)
        assertThat(resolver.resolve("old")).isNull()
    }

    @Test
    fun `expired token and token issued after merge are rejected`() {
        fixture()
        `when`(tokens.validateToken("old")).thenReturn(false)
        assertThat(resolver.resolve("old")).isNull()
        `when`(tokens.validateToken("old")).thenReturn(true)
        `when`(tokens.getIssuedAt("old")).thenReturn(now.plusSeconds(1))
        assertThat(resolver.resolve("old")).isNull()
    }

    @Test
    fun `unlinked identity or deleted retained member cannot be revived`() {
        fixture()
        `when`(identities.isValid(2, LoginIdentity(LoginMethod.KAKAO, 7))).thenReturn(false)
        assertThat(resolver.resolve("old")).isNull()
        `when`(identities.isValid(2, LoginIdentity(LoginMethod.KAKAO, 7))).thenReturn(true)
        `when`(members.isLoginAvailable(2)).thenReturn(false)
        assertThat(resolver.resolve("old")).isNull()
    }

    @Test
    fun `merge chains resolve and cycles are rejected`() {
        fixture()
        `when`(members.isLoginAvailable(2)).thenReturn(false)
        `when`(merges.findTarget(2)).thenReturn(MemberMergeTarget(3, now.plusSeconds(1)))
        `when`(members.isLoginAvailable(3)).thenReturn(true)
        `when`(identities.isValid(3, LoginIdentity(LoginMethod.KAKAO, 7))).thenReturn(true)
        assertThat(resolver.resolve("old")!!.path).containsExactly(1, 2, 3)
        `when`(members.isLoginAvailable(3)).thenReturn(false)
        `when`(merges.findTarget(3)).thenReturn(MemberMergeTarget(1, now.plusSeconds(2)))
        assertThat(resolver.resolve("old")).isNull()
    }
}
