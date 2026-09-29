package core.application.member.application.service

import core.domain.member.aggregate.Member
import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.LoginMethod
import core.domain.member.enums.MemberStatus
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.vo.LoginIdentity
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
    fun `카카오로 로그인하면 로그인한 카카오 연동 정보의 이메일을 반환한다`() {
        val resolver = resolver(oAuths = listOf(oAuth(1L, OAuthProvider.KAKAO, "kakao@kakao.com")))

        assertThat(resolver.resolve(member, LoginIdentity(LoginMethod.KAKAO, 1L))).isEqualTo("kakao@kakao.com")
    }

    @Test
    fun `여러 소셜이 연동되어 있으면 로그인한 연동 정보의 이메일을 반환한다`() {
        val resolver =
            resolver(
                oAuths =
                    listOf(
                        oAuth(1L, OAuthProvider.KAKAO, "kakao@kakao.com"),
                        oAuth(2L, OAuthProvider.APPLE, "abc@privaterelay.appleid.com"),
                    ),
            )

        assertThat(resolver.resolve(member, LoginIdentity(LoginMethod.APPLE, 2L)))
            .isEqualTo("abc@privaterelay.appleid.com")
    }

    @Test
    fun `같은 제공자 연동이 여러 개여도 로그인한 계정의 이메일을 반환한다`() {
        val resolver =
            resolver(
                oAuths =
                    listOf(
                        oAuth(1L, OAuthProvider.KAKAO, "first@kakao.com"),
                        oAuth(5L, OAuthProvider.KAKAO, "second@kakao.com"),
                    ),
            )

        assertThat(resolver.resolve(member, LoginIdentity(LoginMethod.KAKAO, 1L))).isEqualTo("first@kakao.com")
        assertThat(resolver.resolve(member, LoginIdentity(LoginMethod.KAKAO, 5L))).isEqualTo("second@kakao.com")
    }

    @Test
    fun `이메일로 로그인하면 자격 증명의 이메일을 반환한다`() {
        val resolver = resolver(credential = credential(3L, "login@naver.com"))

        assertThat(resolver.resolve(member, LoginIdentity(LoginMethod.EMAIL, 3L))).isEqualTo("login@naver.com")
    }

    @Test
    fun `로그인 계정 정보가 없는 토큰이면 가입 이메일을 반환한다`() {
        val resolver = resolver(oAuths = listOf(oAuth(1L, OAuthProvider.KAKAO, "kakao@kakao.com")))

        assertThat(resolver.resolve(member, null)).isEqualTo("signup@gmail.com")
    }

    @Test
    fun `로그인한 계정의 이메일이 저장되어 있지 않으면 가입 이메일을 반환한다`() {
        val resolver = resolver(oAuths = listOf(oAuth(1L, OAuthProvider.KAKAO, null)))

        assertThat(resolver.resolve(member, LoginIdentity(LoginMethod.KAKAO, 1L))).isEqualTo("signup@gmail.com")
    }

    /** 연동 해제·재연결 등으로 토큰의 계정이 더 이상 이 회원의 것이 아니면 추측하지 않는다. */
    @Test
    fun `토큰의 계정이 이 회원의 것이 아니거나 없으면 가입 이메일을 반환한다`() {
        val resolver =
            resolver(
                oAuths =
                    listOf(
                        oAuth(1L, OAuthProvider.KAKAO, "kakao@kakao.com"),
                        oAuth(9L, OAuthProvider.KAKAO, "other@kakao.com", MemberId(99L)),
                    ),
                credential = credential(3L, "login@naver.com"),
            )

        assertThat(resolver.resolve(member, LoginIdentity(LoginMethod.KAKAO, 9L))).isEqualTo("signup@gmail.com")
        assertThat(resolver.resolve(member, LoginIdentity(LoginMethod.APPLE, 1L))).isEqualTo("signup@gmail.com")
        assertThat(resolver.resolve(member, LoginIdentity(LoginMethod.KAKAO, 404L))).isEqualTo("signup@gmail.com")
        assertThat(resolver.resolve(member, LoginIdentity(LoginMethod.EMAIL, 4L))).isEqualTo("signup@gmail.com")
    }

    private fun oAuth(
        id: Long,
        provider: OAuthProvider,
        email: String?,
        owner: MemberId = memberId,
    ) = MemberOAuth(
        id = MemberOAuthId(id),
        externalId = "$provider-$id",
        provider = provider,
        memberId = owner,
        email = email,
    )

    private fun credential(
        id: Long,
        email: String,
    ) = MemberCredential(
        id = MemberCredentialId(id),
        memberId = memberId,
        email = email,
        password = "encoded",
    )

    private fun resolver(
        oAuths: List<MemberOAuth> = emptyList(),
        credential: MemberCredential? = null,
    ) = MemberLoginEmailResolver(
        memberOAuthPersistencePort = FakeMemberOAuthPersistencePort(oAuths),
        memberCredentialPersistencePort = FakeMemberCredentialPersistencePort(credential),
    )
}

private class FakeMemberOAuthPersistencePort(
    private val oAuths: List<MemberOAuth>,
) : MemberOAuthPersistencePort {
    override fun findById(id: MemberOAuthId): MemberOAuth? = oAuths.firstOrNull { it.id == id }

    override fun save(
        memberOAuth: MemberOAuth,
        member: Member,
    ): MemberOAuth = throw UnsupportedOperationException()

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
    private val credential: MemberCredential?,
) : MemberCredentialPersistencePort {
    override fun findByMemberId(memberId: MemberId): MemberCredential? = credential?.takeIf { it.memberId == memberId }

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
