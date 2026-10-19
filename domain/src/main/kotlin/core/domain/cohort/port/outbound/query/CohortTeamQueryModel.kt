package core.domain.cohort.port.outbound.query

/** 기수에 만들어진 팀 하나. 멤버 배정 여부와 무관하다 */
data class CohortTeamQueryModel(
    val id: Long,
    val number: Int,
)
