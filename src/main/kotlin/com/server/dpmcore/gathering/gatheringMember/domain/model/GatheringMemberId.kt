package com.server.dpmcore.gathering.gatheringMember.domain.model

@JvmInline
value class GatheringMemberId(val value: Long) {
    override fun toString(): String = value.toString()
}
