package core.persistence.image.repository

import core.entity.image.ImageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ImageJpaRepository : JpaRepository<ImageEntity, Long>
