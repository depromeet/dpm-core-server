package com.server.dpmcore.member.memberTeam.domain.model

@JvmInline
value class MemberTeamId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
