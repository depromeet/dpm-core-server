package core.persistence.member.repository.oauth

import core.entity.member.MemberOAuthEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberOAuthJpaRepository : JpaRepository<MemberOAuthEntity, Long>
