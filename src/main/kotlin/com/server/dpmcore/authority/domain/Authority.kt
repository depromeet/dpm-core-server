package com.server.dpmcore.authority.domain

import java.time.LocalDateTime

data class Authority(
    val id: AuthorityId,
    val name: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
