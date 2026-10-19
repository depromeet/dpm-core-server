package core.application.cohort.presentation.response

import core.domain.cohort.port.outbound.query.CohortTeamQueryModel

data class CohortTeamsResponse(
    val teams: List<CohortTeamResponse>,
) {
    data class CohortTeamResponse(
        val id: Long,
        val number: Int,
    )

    companion object {
        fun from(teams: List<CohortTeamQueryModel>): CohortTeamsResponse =
            CohortTeamsResponse(teams.map { CohortTeamResponse(id = it.id, number = it.number) })
    }
}
