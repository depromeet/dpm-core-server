package com.server.dpmcore.team.domain.model

import com.server.dpmcore.cohort.domain.model.CohortId
import com.server.dpmcore.member.memberTeam.domain.MemberTeamId

/**
 * 팀(Team)을 표현하는 도메인 모델입니다.
 *
 * 팀은 각 기수(Cohort) 내에서 구성되는 활동 단위입니다.
 * 하나의 기수에는 여러 개의 팀이 존재할 수 있으며, 팀은 해당 기수에 종속됩니다. (기수:팀 = 1:N)
 *
 * 팀 번호는 정수형으로 저장되며, "1팀", "2팀"과 같은 형태를 숫자로 표현합니다.
 * 예를 들어 number가 1이면 "1팀"을 의미합니다.
 *
 * equals와 hashCode 구현 규칙:
 * - equals는 동일성 판단을 위해 핵심 필드인 id, number, cohortId를 함께 비교합니다.
 * - 두 Team 객체의 id, number, cohortId가 모두 같을 때 동등한 것으로 간주합니다.
 * - hashCode는 id, number, cohortId를 조합하여 계산하며, equals 규칙과 일관성을 유지합니다.
 *
 */
data class Team(
    val id: TeamId? = null,
    val number: Int,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val cohortId: CohortId,
    val memberTeamIds: List<MemberTeamId> = emptyList(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Team) return false

        return id == other.id &&
            number == other.number &&
            cohortId == other.cohortId
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + number.hashCode()
        result = 31 * result + cohortId.hashCode()
        return result
    }
}
