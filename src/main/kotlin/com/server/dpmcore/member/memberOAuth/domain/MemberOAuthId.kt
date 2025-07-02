package com.server.dpmcore.member.memberOAuth.domain

@JvmInline
value class MemberOAuthId(val value: Long) {
    override fun toString(): String = value.toString()
}
