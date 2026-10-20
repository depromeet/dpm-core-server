package core.application.security.oauth.token

import core.domain.member.aggregate.MemberOAuth
import core.domain.member.enums.LoginMethod
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.vo.LoginIdentity
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class LoginIdentityValidatorTest {
    private val oauth = mock(MemberOAuthPersistencePort::class.java)
    private val credentials = mock(MemberCredentialPersistencePort::class.java)
    private val validator = LoginIdentityValidator(oauth, credentials)

    @Test
    fun `식별자가 없거나 해제된 토큰은 사용하지 못한다`() {
        assertThat(validator.isValid(1, null)).isFalse()
        assertThat(validator.isValid(1, LoginIdentity(LoginMethod.APPLE, 10))).isFalse()
    }

    @Test
    fun `회원 통합으로 소유자가 달라지거나 제공자가 다르면 토큰을 거절한다`() {
        `when`(oauth.findById(MemberOAuthId(10))).thenReturn(MemberOAuth(MemberOAuthId(10), "external", OAuthProvider.KAKAO, MemberId(2)))
        assertThat(validator.isValid(1, LoginIdentity(LoginMethod.KAKAO, 10))).isFalse()
        assertThat(validator.isValid(2, LoginIdentity(LoginMethod.APPLE, 10))).isFalse()
        assertThat(validator.isValid(2, LoginIdentity(LoginMethod.KAKAO, 10))).isTrue()
    }
}
