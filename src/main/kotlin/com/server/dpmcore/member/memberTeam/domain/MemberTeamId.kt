package com.server.dpmcore.member.memberTeam.domain

import java.util.UUID

@JvmInline
value class MemberTeamId(val value: UUID) {
    companion object {
        fun generate(): MemberTeamId = MemberTeamId(UUID.randomUUID())
    }

    override fun toString(): String = value.toString()
}
