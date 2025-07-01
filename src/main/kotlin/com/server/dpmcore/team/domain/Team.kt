package com.server.dpmcore.team.domain

import com.server.dpmcore.cohort.domain.CohortId
import java.time.LocalDateTime

data class Team(
    val id: TeamId,
    val number: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val cohortId: CohortId,
)
