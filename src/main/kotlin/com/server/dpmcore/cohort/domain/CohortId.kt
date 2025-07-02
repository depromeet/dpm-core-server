package com.server.dpmcore.cohort.domain

import java.util.UUID

@JvmInline
value class CohortId(val value: UUID) {
    companion object {
        fun generate(): CohortId = CohortId(UUID.randomUUID())
    }

    override fun toString(): String = value.toString()
}
