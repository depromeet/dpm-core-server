package com.server.dpmcore.team.domain.model

@JvmInline
value class TeamId(val value: Long) {
    override fun toString(): String = value.toString()
}
