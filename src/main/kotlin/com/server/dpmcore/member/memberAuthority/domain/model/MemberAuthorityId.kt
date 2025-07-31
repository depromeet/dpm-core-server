package com.server.dpmcore.member.memberAuthority.domain.model

@JvmInline
value class MemberAuthorityId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
