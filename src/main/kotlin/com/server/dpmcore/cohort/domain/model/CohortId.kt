package com.server.dpmcore.cohort.domain.model

@JvmInline
value class CohortId(val value: Long) {
    override fun toString(): String = value.toString()
}
