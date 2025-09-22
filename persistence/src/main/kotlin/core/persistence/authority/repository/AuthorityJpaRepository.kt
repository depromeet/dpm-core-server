package core.persistence.authority.repository

import core.persistence.authority.entity.AuthorityEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AuthorityJpaRepository : JpaRepository<AuthorityEntity, Long>
