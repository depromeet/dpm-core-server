package core.persistence.member.repository.team

import com.server.dpmcore.member.memberOAuth.infrastructure.entity.MemberOAuthEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberOAuthJpaRepository : JpaRepository<MemberOAuthEntity, Long>
