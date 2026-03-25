package core.application.attendance.presentation.response

import core.domain.team.vo.TeamNumber

data class MyTeamFilterResponse(
    val teamNumber: TeamNumber?,
    val isMyTeam: Boolean,
)
