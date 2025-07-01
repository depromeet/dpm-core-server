package com.server.dpmcore.member.memberOAuth.domain

import com.server.dpmcore.member.member.domain.MemberId

data class MemberOAuth(
    val id: MemberOAuthId,
    val externalId: String,
    val provider: OAuthProvider,
    val memberId: MemberId,
)
