package core.persistence.image.repository

import core.entity.image.ImageUploadEntity
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** 상태 전이는 모두 현재 상태(와 lease token)를 조건으로 거는 UPDATE 다. 반영 행 수 0 은 경쟁에서 진 것이다. */
interface ImageUploadJpaRepository : JpaRepository<ImageUploadEntity, String> {
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ImageUploadEntity u
        set u.status = 'VERIFYING', u.leaseToken = :token, u.leaseUntil = :leaseUntil
        where u.id = :id
          and u.expiresAt > :now
          and (u.status = 'PENDING' or (u.status = 'VERIFYING' and (u.leaseUntil is null or u.leaseUntil <= :now)))
        """,
    )
    fun startVerification(
        @Param("id") id: String,
        @Param("token") token: String,
        @Param("now") now: Instant,
        @Param("leaseUntil") leaseUntil: Instant,
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ImageUploadEntity u
        set u.status = 'PENDING', u.leaseToken = null, u.leaseUntil = null
        where u.id = :id and u.status = 'VERIFYING' and u.leaseToken = :token
        """,
    )
    fun releaseVerification(
        @Param("id") id: String,
        @Param("token") token: String,
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ImageUploadEntity u
        set u.status = 'COPYING', u.etag = :etag, u.leaseUntil = :leaseUntil
        where u.id = :id and u.status = 'VERIFYING' and u.leaseToken = :token
        """,
    )
    fun markValidated(
        @Param("id") id: String,
        @Param("token") token: String,
        @Param("etag") etag: String,
        @Param("leaseUntil") leaseUntil: Instant,
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ImageUploadEntity u
        set u.status = 'REJECTED', u.failureCode = :failureCode, u.leaseToken = null, u.leaseUntil = null
        where u.id = :id and u.status = 'VERIFYING' and u.leaseToken = :token
        """,
    )
    fun reject(
        @Param("id") id: String,
        @Param("token") token: String,
        @Param("failureCode") failureCode: String,
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ImageUploadEntity u
        set u.leaseToken = :token, u.leaseUntil = :leaseUntil
        where u.id = :id and u.status = 'COPYING'
          and (u.leaseToken is null or u.leaseUntil is null or u.leaseUntil <= :now)
        """,
    )
    fun acquireCopy(
        @Param("id") id: String,
        @Param("token") token: String,
        @Param("now") now: Instant,
        @Param("leaseUntil") leaseUntil: Instant,
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ImageUploadEntity u
        set u.workRequestId = :workRequestId
        where u.id = :id and u.status = 'COPYING' and u.leaseToken = :token
        """,
    )
    fun recordCopyWorkRequest(
        @Param("id") id: String,
        @Param("token") token: String,
        @Param("workRequestId") workRequestId: String,
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ImageUploadEntity u
        set u.leaseToken = null, u.leaseUntil = null
        where u.id = :id and u.status = 'COPYING' and u.leaseToken = :token
        """,
    )
    fun releaseCopy(
        @Param("id") id: String,
        @Param("token") token: String,
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ImageUploadEntity u
        set u.status = 'FAILED', u.failureCode = :failureCode, u.leaseToken = null, u.leaseUntil = null
        where u.id = :id and u.status = 'COPYING' and u.leaseToken = :token
        """,
    )
    fun fail(
        @Param("id") id: String,
        @Param("token") token: String,
        @Param("failureCode") failureCode: String,
    ): Int

    /** 완료 직전 같은 조건(COPYING, lease token)을 행 잠금으로 확인한다. 잠금은 호출 트랜잭션 끝까지 유지된다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from ImageUploadEntity u where u.id = :id and u.status = 'COPYING' and u.leaseToken = :token")
    fun findCompletableForUpdate(
        @Param("id") id: String,
        @Param("token") token: String,
    ): ImageUploadEntity?

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ImageUploadEntity u
        set u.status = 'COMPLETED', u.imageId = :imageId, u.leaseToken = null, u.leaseUntil = null
        where u.id = :id and u.status = 'COPYING' and u.leaseToken = :token
        """,
    )
    fun markCompleted(
        @Param("id") id: String,
        @Param("token") token: String,
        @Param("imageId") imageId: Long,
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ImageUploadEntity u set u.parId = null where u.id = :id and u.status = 'COMPLETED'")
    fun clearParId(
        @Param("id") id: String,
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ImageUploadEntity u
        set u.status = 'EXPIRED', u.leaseToken = null, u.leaseUntil = null
        where u.id = :id and u.status in ('PENDING', 'VERIFYING')
          and (u.leaseToken is null or u.leaseUntil is null or u.leaseUntil <= :now)
        """,
    )
    fun expire(
        @Param("id") id: String,
        @Param("now") now: Instant,
    ): Int

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ImageUploadEntity u where u.id = :id and u.status in ('REJECTED', 'FAILED', 'EXPIRED')")
    fun deleteTerminal(
        @Param("id") id: String,
    ): Int

    @Query(
        """
        select u from ImageUploadEntity u
        where u.parId is not null and u.createdAt < :createdBefore
        order by u.createdAt
        """,
    )
    fun findStale(
        @Param("createdBefore") createdBefore: Instant,
        pageable: Pageable,
    ): List<ImageUploadEntity>
}
