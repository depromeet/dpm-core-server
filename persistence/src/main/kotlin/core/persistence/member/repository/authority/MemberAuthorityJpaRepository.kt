package core.persistence.member.repository.authority

import core.entity.member.MemberAuthorityEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberAuthorityJpaRepository : JpaRepository<MemberAuthorityEntity, Long>
