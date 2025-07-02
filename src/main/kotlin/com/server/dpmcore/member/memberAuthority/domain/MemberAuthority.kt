package com.server.dpmcore.member.memberAuthority.domain

import com.server.dpmcore.authority.domain.model.AuthorityId
import com.server.dpmcore.member.member.domain.model.MemberId
import java.time.LocalDateTime

data class MemberAuthority(
    val id: MemberAuthorityId = MemberAuthorityId.generate(),
    val memberId: MemberId,
    val authorityId: AuthorityId,
    val grantedAt: LocalDateTime? = null,
    val deletedAt: LocalDateTime? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemberAuthority

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
