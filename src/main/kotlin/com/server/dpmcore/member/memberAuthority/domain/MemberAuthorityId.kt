package com.server.dpmcore.member.memberAuthority.domain

@JvmInline
value class MemberAuthorityId(val value: Long) {
    override fun toString(): String = value.toString()
}
