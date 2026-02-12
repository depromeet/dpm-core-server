package core.persistence.membercredential.repository

import core.entity.membercredential.MemberCredentialEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberCredentialJpaRepository : JpaRepository<MemberCredentialEntity, Long> {
    fun findByEmail(email: String): MemberCredentialEntity?

    fun findByMemberId(memberId: Long): MemberCredentialEntity?

    fun existsByEmail(email: String): Boolean
}
