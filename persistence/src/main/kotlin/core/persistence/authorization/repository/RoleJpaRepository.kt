package core.persistence.authorization.repository

import core.entity.authorization.RoleEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RoleJpaRepository : JpaRepository<RoleEntity, Long> {
    fun findByName(name: String): Optional<RoleEntity>
}
