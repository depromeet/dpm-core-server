package com.server.dpmcore.member.memberAuthority.domain

import com.server.dpmcore.authority.domain.AuthorityId
import com.server.dpmcore.member.member.domain.MemberId

data class MemberAuthority(
    val id: MemberAuthorityId,
    val memberId: MemberId,
    val authorityId: AuthorityId,
)
