package com.server.dpmcore.member.member.domain.model

import java.util.UUID

@JvmInline
value class MemberId(val value: UUID) {
    companion object {
        fun generate(): MemberId = MemberId(UUID.randomUUID())
    }

    override fun toString(): String = value.toString()
}
