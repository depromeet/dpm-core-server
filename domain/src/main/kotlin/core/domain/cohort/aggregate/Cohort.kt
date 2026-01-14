package core.domain.cohort.aggregate

import core.domain.cohort.vo.CohortId
import core.domain.member.vo.MemberCohortId
import core.domain.team.vo.TeamId

/**
 * 기수(Cohort)를 표현하는 도메인 모델입니다.
 *
 * 기수는 조직 또는 활동 주기의 단위를 의미하며, 회원이 어느 시점의 활동에 소속되어 있었는지를 나타냅니다.
 * 예를 들어 일반 활동 기수는 "16기", "17기"와 같은 형식으로, 코어 기수는 "코어 1기"로 저장됩니다.
 *
 * 기수 값은 문자열로 저장되며, 각 기수는 고유하고 중복될 수 없습니다.
 * 한 명의 회원은 여러 기수를 수료할 수 있으며, 활동 이력으로서 다수의 기수에 소속될 수 있습니다.
 *
 * equals와 hashCode 구현 규칙:
 * - equals는 동일성 판단을 위해 핵심 필드인 id와 value를 함께 비교합니다.
 * - 두 Cohort 객체의 id와 value가 모두 같을 때 동등한 것으로 간주합니다.
 * - hashCode는 id와 value를 조합하여 계산하며, equals 규칙과 일관성을 유지합니다.
 *
 */
class Cohort(
    val id: CohortId? = null,
    val value: String,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val teamIds: List<TeamId> = emptyList(),
    val memberCohortIds: List<MemberCohortId> = emptyList(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Cohort) return false

        return id == other.id && value == other.value
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + value.hashCode()
        return result
    }

    override fun toString(): String =
        "Cohort(id=$id, value='$value', createdAt=$createdAt, updatedAt=$updatedAt, teamIds=$teamIds, " +
            "memberCohortIds=$memberCohortIds)"
}
