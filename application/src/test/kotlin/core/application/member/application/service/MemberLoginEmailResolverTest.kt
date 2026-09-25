package core.application.member.application.service

import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.LoginMethod
import core.domain.member.enums.MemberStatus
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import core.domain.membercredential.aggregate.MemberCredential
import core.domain.membercredential.aggregate.MemberCredentialId
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class MemberLoginEmailResolverTest {
    private val memberId = MemberId(10L)
    private val member =
        Member(
            id = memberId,
            name = "디프만",
            signupEmail = "signup@gmail.com",
            status = MemberStatus.ACTIVE,
        )

    @Test
    fun `카카오로 로그인하면 카카오 연동 정보의 이메일을 반환한다`() {
        val resolver = resolver(oAuths = listOf(oAuth(1L, OAuthProvider.KAKAO, "kakao@kakao.com")))

        assertThat(resolver.resolve(member, LoginMethod.KAKAO)).isEqualTo("kakao@kakao.com")
    }

    @Test
    fun `여러 소셜이 연동되어 있으면 현재 로그인한 제공자의 이메일을 반환한다`() {
        val resolver =
            resolver(
                oAuths =
                    listOf(
                        oAuth(1L, OAuthProvider.KAKAO, "kakao@kakao.com"),
                        oAuth(2L, OAuthProvider.APPLE, "abc@privaterelay.appleid.com"),
                    ),
            )

        assertThat(resolver.resolve(member, LoginMethod.APPLE)).isEqualTo("abc@privaterelay.appleid.com")
    }

    @Test
    fun `이메일로 로그인하면 자격 증명의 이메일을 반환한다`() {
        val resolver = resolver(credentialEmail = "login@naver.com")

        assertThat(resolver.resolve(member, LoginMethod.EMAIL)).isEqualTo("login@naver.com")
    }

    @Test
    fun `로그인 수단이 없는 토큰이면 가입 이메일을 반환한다`() {
        val resolver = resolver(oAuths = listOf(oAuth(1L, OAuthProvider.KAKAO, "kakao@kakao.com")))

        assertThat(resolver.resolve(member, null)).isEqualTo("signup@gmail.com")
    }

    @Test
    fun `로그인 수단의 이메일이 저장되어 있지 않으면 가입 이메일을 반환한다`() {
        val resolver = resolver(oAuths = listOf(oAuth(1L, OAuthProvider.KAKAO, null)))

        assertThat(resolver.resolve(member, LoginMethod.KAKAO)).isEqualTo("signup@gmail.com")
        assertThat(resolver.resolve(member, LoginMethod.EMAIL)).isEqualTo("signup@gmail.com")
    }

    @Test
    fun `같은 제공자 연동이 여러 개면 가장 최근에 연동된 이메일을 반환한다`() {
        val resolver =
            resolver(
                oAuths =
                    listOf(
                        oAuth(1L, OAuthProvider.KAKAO, "old@kakao.com"),
                        oAuth(5L, OAuthProvider.KAKAO, "new@kakao.com"),
                    ),
            )

        assertThat(resolver.resolve(member, LoginMethod.KAKAO)).isEqualTo("new@kakao.com")
    }

    private fun oAuth(
        id: Long,
        provider: OAuthProvider,
        email: String?,
    ) = MemberOAuth(
        id = MemberOAuthId(id),
        externalId = "$provider-$id",
        provider = provider,
        memberId = memberId,
        email = email,
    )

    private fun resolver(
        oAuths: List<MemberOAuth> = emptyList(),
        credentialEmail: String? = null,
    ) = MemberLoginEmailResolver(
        memberOAuthPersistencePort = FakeMemberOAuthPersistencePort(oAuths),
        memberCredentialPersistencePort = FakeMemberCredentialPersistencePort(memberId, credentialEmail),
    )
}

private class FakeMemberOAuthPersistencePort(
    private val oAuths: List<MemberOAuth>,
) : MemberOAuthPersistencePort {
    override fun findLatestByMemberIdAndProvider(
        memberId: MemberId,
        provider: OAuthProvider,
    ): MemberOAuth? =
        oAuths
            .filter { it.memberId == memberId && it.provider == provider }
            .maxByOrNull { it.id!!.value }

    override fun save(
        memberOAuth: MemberOAuth,
        member: Member,
    ) = throw UnsupportedOperationException()

    override fun relinkToMember(
        provider: OAuthProvider,
        externalId: String,
        member: Member,
    ) = throw UnsupportedOperationException()

    override fun findByProviderAndExternalId(
        provider: OAuthProvider,
        externalId: String,
    ): MemberOAuth? = throw UnsupportedOperationException()

    override fun findMemberIdsByProvider(provider: OAuthProvider): List<MemberId> = throw UnsupportedOperationException()

    override fun updateEmail(
        provider: OAuthProvider,
        externalId: String,
        email: String,
    ) = throw UnsupportedOperationException()

    override fun deleteAllByMemberId(memberId: MemberId) = throw UnsupportedOperationException()
}

private class FakeMemberCredentialPersistencePort(
    private val memberId: MemberId,
    private val email: String?,
) : MemberCredentialPersistencePort {
    override fun findByMemberId(memberId: MemberId): MemberCredential? =
        email?.takeIf { memberId == this.memberId }?.let {
            MemberCredential(
                id = MemberCredentialId(1L),
                memberId = memberId,
                email = it,
                password = "encoded",
            )
        }

    override fun save(credential: MemberCredential): MemberCredential = throw UnsupportedOperationException()

    override fun findByEmail(email: String): MemberCredential? = throw UnsupportedOperationException()

    override fun existsByEmail(email: String): Boolean = throw UnsupportedOperationException()

    override fun updatePassword(
        credentialId: MemberCredentialId,
        encodedPassword: String,
        updatedAt: Instant,
    ) = throw UnsupportedOperationException()

    override fun deleteByMemberId(memberId: MemberId) = throw UnsupportedOperationException()
}
