package core.application.member.application.service

import core.domain.member.aggregate.Member
import core.domain.member.enums.LoginMethod
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.vo.LoginIdentity
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import org.springframework.stereotype.Component

/**
 * 현재 세션에 로그인한 계정의 이메일을 고른다.
 *
 * - KAKAO / APPLE: 토큰에 담긴 member_oauth_id 의 연동 정보 이메일
 * - EMAIL: 토큰에 담긴 member_credential_id 의 자격 증명 이메일
 *
 * 로그인 계정이 없는 토큰(배포 전 발급분)이거나, 그 계정이 더 이상 이 회원의 것이 아니거나,
 * 이메일이 저장되어 있지 않으면 추측하지 않고 가입 이메일로 대신한다.
 */
@Component
class MemberLoginEmailResolver(
    private val memberOAuthPersistencePort: MemberOAuthPersistencePort,
    private val memberCredentialPersistencePort: MemberCredentialPersistencePort,
) {
    fun resolve(
        member: Member,
        loginIdentity: LoginIdentity?,
    ): String {
        val memberId = requireNotNull(member.id) { "Member must have id" }
        val loginEmail =
            when (loginIdentity?.method) {
                LoginMethod.KAKAO, LoginMethod.APPLE -> findOAuthEmail(memberId, loginIdentity)
                LoginMethod.EMAIL -> findCredentialEmail(memberId, loginIdentity)
                null -> null
            }
        return loginEmail ?: member.signupEmail
    }

    private fun findOAuthEmail(
        memberId: MemberId,
        loginIdentity: LoginIdentity,
    ): String? =
        memberOAuthPersistencePort
            .findById(MemberOAuthId(loginIdentity.accountId))
            ?.takeIf { it.memberId == memberId && LoginMethod.from(it.provider) == loginIdentity.method }
            ?.email

    private fun findCredentialEmail(
        memberId: MemberId,
        loginIdentity: LoginIdentity,
    ): String? =
        memberCredentialPersistencePort
            .findByMemberId(memberId)
            ?.takeIf { it.id?.value == loginIdentity.accountId }
            ?.email
}
