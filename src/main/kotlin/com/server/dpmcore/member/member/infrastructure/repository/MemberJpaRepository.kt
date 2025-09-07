package com.server.dpmcore.member.member.infrastructure.repository

import com.server.dpmcore.member.member.infrastructure.entity.MemberEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberJpaRepository : JpaRepository<MemberEntity, Long> {
    fun findByEmail(email: String): MemberEntity

    fun existsByIdAndDeletedAtIsNotNull(memberId: Long): Boolean
}
