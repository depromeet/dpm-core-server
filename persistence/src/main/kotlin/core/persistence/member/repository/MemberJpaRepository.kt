package core.persistence.member.repository

import core.entity.member.MemberEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberJpaRepository : JpaRepository<MemberEntity, Long> {
    fun findByEmail(email: String): MemberEntity

    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<MemberEntity>

    fun existsByIdAndDeletedAtIsNotNull(memberId: Long): Boolean
}
