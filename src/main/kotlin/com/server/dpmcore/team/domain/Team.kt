package com.server.dpmcore.team.domain

import com.server.dpmcore.cohort.domain.CohortId
import java.time.LocalDateTime

/**
 * 팀(Team)을 표현하는 도메인 모델입니다.
 *
 * 팀은 각 기수(Cohort) 내에서 구성되는 활동 단위입니다.
 * 하나의 기수에는 여러 개의 팀이 존재할 수 있으며, 팀은 해당 기수에 종속됩니다. (기수:팀 = 1:N)
 *
 * 팀 번호는 정수형으로 저장되며, "1팀", "2팀"과 같은 형태를 숫자로 표현합니다.
 * 예를 들어 number가 1이면 "1팀"을 의미합니다.
 */
data class Team(
    val id: TeamId,
    val number: Int,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val cohortId: CohortId,
)
