package com.server.dpmcore.member.memberOAuth.infrastructure.repository

import com.server.dpmcore.member.memberOAuth.infrastructure.entity.MemberOAuthEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberOAuthJpaRepository : JpaRepository<MemberOAuthEntity, Long>
