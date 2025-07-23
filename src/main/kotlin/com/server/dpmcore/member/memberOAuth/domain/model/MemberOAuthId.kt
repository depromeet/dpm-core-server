package com.server.dpmcore.member.memberOAuth.domain.model

@JvmInline
value class MemberOAuthId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
