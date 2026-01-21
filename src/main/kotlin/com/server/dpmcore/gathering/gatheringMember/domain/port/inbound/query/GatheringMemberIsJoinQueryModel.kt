package com.server.dpmcore.gathering.gatheringMember.domain.port.inbound.query

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GatheringMemberIsJoinQueryModel(
    val name: String,
    val authority: String,
    val part: String?,
    val isJoined: Boolean,
) {
    companion object {
        fun of(
            name: String,
            authority: String,
            part: String?,
            isJoined: Boolean,
        ) = GatheringMemberIsJoinQueryModel(
            name = name,
            authority = authority,
            part = part,
            isJoined = isJoined,
        )
    }
}
