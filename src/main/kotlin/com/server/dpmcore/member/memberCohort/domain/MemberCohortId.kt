package com.server.dpmcore.member.memberCohort.domain

import java.util.UUID

@JvmInline
value class MemberCohortId(val value: UUID) {
    companion object {
        fun generate(): MemberCohortId = MemberCohortId(UUID.randomUUID())
    }

    override fun toString(): String = value.toString()
}
