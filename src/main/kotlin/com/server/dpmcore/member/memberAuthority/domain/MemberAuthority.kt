package com.server.dpmcore.member.memberAuthority.domain

import com.server.dpmcore.authority.domain.AuthorityId
import com.server.dpmcore.member.member.domain.MemberId
import java.time.LocalDateTime

data class MemberAuthority(
    val id: MemberAuthorityId,
    val memberId: MemberId,
    val authorityId: AuthorityId,
    val grantedAt: LocalDateTime? = null,
    val deletedAt: LocalDateTime? = null,
)
