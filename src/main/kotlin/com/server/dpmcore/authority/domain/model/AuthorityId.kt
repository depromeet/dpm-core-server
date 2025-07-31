package com.server.dpmcore.authority.domain.model

@JvmInline
value class AuthorityId(val value: Long) {
    override fun toString(): String = value.toString()
}
