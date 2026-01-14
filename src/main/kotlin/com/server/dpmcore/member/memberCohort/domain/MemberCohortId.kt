package com.server.dpmcore.member.memberCohort.domain

@JvmInline
value class MemberCohortId(val value: Long) {
    override fun toString(): String = value.toString()
}
