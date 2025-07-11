package com.server.dpmcore.session.domain.model

@JvmInline
value class SessionId(
    val value: Long,
) {
    override fun toString(): String = value.toString()
}
