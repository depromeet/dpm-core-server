package com.server.dpmcore.member.member.domain.model

import com.server.dpmcore.member.member.domain.exception.InvalidMemberIdException

@JvmInline
value class MemberId(
    val value: Long,
) {
    init {
        if (value <= 0) {
            throw InvalidMemberIdException()
        }
    }

    override fun toString(): String = value.toString()
}
