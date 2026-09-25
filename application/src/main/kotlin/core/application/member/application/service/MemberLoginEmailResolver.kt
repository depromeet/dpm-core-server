package core.application.member.application.service

import core.domain.member.aggregate.Member
import core.domain.member.enums.LoginMethod
import core.domain.member.enums.OAuthProvider
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.vo.MemberId
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import org.springframework.stereotype.Component

/**
 * 현재 세션의 로그인 수단에 해당하는 이메일을 고른다.
 *
 * - KAKAO / APPLE: 해당 제공자 연동 정보(member_oauth)의 이메일. 같은 제공자 연동이 여럿이면 가장 최근 것.
 * - EMAIL: 이메일/비밀번호 자격 증명의 이메일.
 * 로그인 수단이 없는 토큰(배포 전 발급분)이거나 이메일이 저장되어 있지 않으면 가입 이메일로 대신한다.
 */
@Component
class MemberLoginEmailResolver(
    private val memberOAuthPersistencePort: MemberOAuthPersistencePort,
    private val memberCredentialPersistencePort: MemberCredentialPersistencePort,
) {
    fun resolve(
        member: Member,
        loginMethod: LoginMethod?,
    ): String {
        val memberId = requireNotNull(member.id) { "Member must have id" }
        val loginEmail =
            when (loginMethod) {
                LoginMethod.KAKAO -> findOAuthEmail(memberId, OAuthProvider.KAKAO)
                LoginMethod.APPLE -> findOAuthEmail(memberId, OAuthProvider.APPLE)
                LoginMethod.EMAIL -> memberCredentialPersistencePort.findByMemberId(memberId)?.email
                null -> null
            }
        return loginEmail ?: member.signupEmail
    }

    private fun findOAuthEmail(
        memberId: MemberId,
        provider: OAuthProvider,
    ): String? = memberOAuthPersistencePort.findLatestByMemberIdAndProvider(memberId, provider)?.email
}
