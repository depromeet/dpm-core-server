package core.persistence.authority.repository

import core.entity.authority.AuthorityEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AuthorityJpaRepository : JpaRepository<AuthorityEntity, Long>
