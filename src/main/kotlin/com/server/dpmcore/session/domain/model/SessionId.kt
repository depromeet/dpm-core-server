package com.server.dpmcore.session.domain.model

import com.server.dpmcore.session.domain.exception.InvalidSessionIdException

@JvmInline
value class SessionId(
    val value: Long,
) {
    init {
        if (value <= 0) {
            throw InvalidSessionIdException()
        }
    }

    override fun toString(): String = value.toString()
}
