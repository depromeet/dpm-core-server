package com.server.dpmcore.member.member.domain.port.outbound

import com.server.dpmcore.member.member.domain.model.Member

interface MemberPersistencePort {
    fun save(member: Member): Long

    fun findByEmail(email: String): Member?

    fun findById(memberId: Long): Member?

    fun delete(memberId: Long)

    fun existsById(memberId: Long): Boolean

    fun existsDeletedMemberById(memberId: Long): Boolean
}
