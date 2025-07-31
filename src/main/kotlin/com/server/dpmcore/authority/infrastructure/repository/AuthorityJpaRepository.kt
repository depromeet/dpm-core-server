package com.server.dpmcore.authority.infrastructure.repository

import com.server.dpmcore.authority.infrastructure.entity.AuthorityEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AuthorityJpaRepository : JpaRepository<AuthorityEntity, Long>
