package com.server.dpmcore.member.memberAuthority.infrastructure.repository

import com.server.dpmcore.member.memberAuthority.infrastructure.entity.MemberAuthorityEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberAuthorityJpaRepository : JpaRepository<MemberAuthorityEntity, Long>
