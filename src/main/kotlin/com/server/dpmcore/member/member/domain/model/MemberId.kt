package com.server.dpmcore.member.member.domain.model

@JvmInline
value class MemberId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
