package core.persistence.member.repository.team

import core.entity.member.MemberOAuthEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberOAuthJpaRepository : JpaRepository<MemberOAuthEntity, Long>
