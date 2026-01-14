package core.persistence.authorization.repository

import core.entity.authorization.RoleEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RoleJpaRepository : JpaRepository<RoleEntity, Long>
