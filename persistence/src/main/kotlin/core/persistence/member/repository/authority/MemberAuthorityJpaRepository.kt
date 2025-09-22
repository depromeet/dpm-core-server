package core.persistence.member.repository.authority

import com.server.dpmcore.member.memberAuthority.infrastructure.entity.MemberAuthorityEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberAuthorityJpaRepository : JpaRepository<MemberAuthorityEntity, Long>
