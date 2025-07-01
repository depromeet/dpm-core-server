package com.server.dpmcore.member.memberAuthority.domain

import java.util.UUID

@JvmInline
value class MemberAuthorityId(private val value: UUID) {
    companion object {
        fun generate(): MemberAuthorityId = MemberAuthorityId(UUID.randomUUID())
    }

    override fun toString(): String = value.toString()
}

