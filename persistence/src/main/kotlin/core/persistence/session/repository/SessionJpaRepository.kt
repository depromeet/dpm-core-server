package core.persistence.session.repository

import core.entity.session.SessionEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface SessionJpaRepository : JpaRepository<SessionEntity, Long> {
    fun findFirstByDateAfterAndDeletedAtIsNullOrderByDateAsc(startOfToday: Instant): SessionEntity?

    fun findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(cohortId: Long): List<SessionEntity>

    fun findByIdAndDeletedAtIsNull(id: Long): SessionEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessionEntity s where s.id = :id and s.deletedAt is null")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): SessionEntity?

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select s from SessionEntity s where s.id = :id and s.deletedAt is null")
    fun findByIdForShare(
        @Param("id") id: Long,
    ): SessionEntity?

}
