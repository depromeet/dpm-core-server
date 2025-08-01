package com.server.dpmcore.member.member.presentation.request

import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.domain.model.MemberPart
import com.server.dpmcore.team.domain.model.TeamId

data class InitMemberDataRequest(
    val teamId: TeamId,
    val members: List<MemberData>,
) {
    data class MemberData(
        val memberId: MemberId,
        val memberPart: MemberPart,
    )
}
