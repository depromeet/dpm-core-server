package com.server.dpmcore.member.member.domain

import java.time.LocalDateTime

data class Member(
    val id: MemberId,
    val name: String,
    val email: String,
    val part: MemberPart,
    val status: MemberStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)
