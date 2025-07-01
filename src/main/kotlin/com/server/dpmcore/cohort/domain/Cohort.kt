package com.server.dpmcore.cohort.domain

import java.time.LocalDateTime

data class Cohort(
    val id: CohortId,
    val number: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
