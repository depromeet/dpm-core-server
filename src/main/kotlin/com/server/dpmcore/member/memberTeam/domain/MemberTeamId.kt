package com.server.dpmcore.member.memberTeam.domain

@JvmInline
value class MemberTeamId(val value: Long) {
    override fun toString(): String = value.toString()
}
