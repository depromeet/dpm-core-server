package core.application.member.presentation.response

import core.domain.member.enums.LoginMethod

data class MemberAccountResponse(val accounts: List<ConnectedMemberAccount>)

data class ConnectedMemberAccount(
    val accountId: Long,
    val loginMethod: LoginMethod,
    val email: String?,
    val unlinkAllowed: Boolean,
)
