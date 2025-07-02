package com.server.dpmcore.member.memberOAuth.domain

import java.util.UUID

@JvmInline
value class MemberOAuthId(val value: UUID) {
    companion object {
        fun generate(): MemberOAuthId = MemberOAuthId(UUID.randomUUID())
    }

    override fun toString(): String = value.toString()
}
