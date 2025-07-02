package com.server.dpmcore.team.domain.model

import java.util.UUID

@JvmInline
value class TeamId(val value: UUID) {
    companion object {
        fun generate(): TeamId = TeamId(UUID.randomUUID())
    }

    override fun toString(): String = value.toString()
}
