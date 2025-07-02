package com.server.dpmcore.authority.domain.model

import java.util.UUID

@JvmInline
value class AuthorityId(val value: UUID) {
    companion object {
        fun generate(): AuthorityId = AuthorityId(UUID.randomUUID())
    }

    override fun toString(): String = value.toString()
}
