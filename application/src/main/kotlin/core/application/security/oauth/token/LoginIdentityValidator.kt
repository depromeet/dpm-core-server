package core.application.security.oauth.token

import core.domain.member.enums.LoginMethod
import core.domain.member.port.outbound.MemberOAuthPersistencePort
import core.domain.member.vo.LoginIdentity
import core.domain.member.vo.MemberId
import core.domain.member.vo.MemberOAuthId
import core.domain.membercredential.port.outbound.MemberCredentialPersistencePort
import org.springframework.stereotype.Component

@Component
class LoginIdentityValidator(
    private val oauth: MemberOAuthPersistencePort,
    private val credentials: MemberCredentialPersistencePort,
) {
    fun isValid(
        memberId: Long,
        identity: LoginIdentity?,
    ): Boolean {
        if (identity == null) return false
        if (identity.method == LoginMethod.EMAIL) {
            return credentials.findByMemberId(MemberId(memberId))?.let {
                it.id?.value == identity.accountId && it.password.isNotBlank()
            } == true
        }
        return oauth.findById(MemberOAuthId(identity.accountId))?.let {
            it.memberId.value == memberId && LoginMethod.from(it.provider) == identity.method
        } == true
    }
}
