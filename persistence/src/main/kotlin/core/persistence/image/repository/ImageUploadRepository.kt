package core.persistence.image.repository

import core.domain.image.aggregate.Image
import core.domain.image.aggregate.ImageUpload
import core.domain.image.port.outbound.ImageUploadPersistencePort
import core.entity.image.ImageEntity
import core.entity.image.ImageUploadEntity
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport
import java.time.Instant

@Repository
class ImageUploadRepository(
    private val imageUploadJpaRepository: ImageUploadJpaRepository,
    private val imageJpaRepository: ImageJpaRepository,
) : ImageUploadPersistencePort {
    override fun save(upload: ImageUpload): ImageUpload =
        imageUploadJpaRepository.save(ImageUploadEntity.from(upload)).toDomain()

    override fun findById(uploadId: String): ImageUpload? =
        imageUploadJpaRepository.findById(uploadId).orElse(null)?.toDomain()

    override fun startVerification(
        uploadId: String,
        token: String,
        now: Instant,
        leaseUntil: Instant,
    ): Boolean = imageUploadJpaRepository.startVerification(uploadId, token, now, leaseUntil) == 1

    override fun releaseVerification(
        uploadId: String,
        token: String,
    ): Boolean = imageUploadJpaRepository.releaseVerification(uploadId, token) == 1

    override fun markValidated(
        uploadId: String,
        token: String,
        etag: String,
        leaseUntil: Instant,
    ): Boolean = imageUploadJpaRepository.markValidated(uploadId, token, etag, leaseUntil) == 1

    override fun reject(
        uploadId: String,
        token: String,
        failureCode: String,
    ): Boolean = imageUploadJpaRepository.reject(uploadId, token, failureCode) == 1

    override fun acquireCopy(
        uploadId: String,
        token: String,
        now: Instant,
        leaseUntil: Instant,
    ): Boolean = imageUploadJpaRepository.acquireCopy(uploadId, token, now, leaseUntil) == 1

    override fun recordCopyWorkRequest(
        uploadId: String,
        token: String,
        workRequestId: String,
    ): Boolean = imageUploadJpaRepository.recordCopyWorkRequest(uploadId, token, workRequestId) == 1

    override fun releaseCopy(
        uploadId: String,
        token: String,
    ): Boolean = imageUploadJpaRepository.releaseCopy(uploadId, token) == 1

    override fun fail(
        uploadId: String,
        token: String,
        failureCode: String,
    ): Boolean = imageUploadJpaRepository.fail(uploadId, token, failureCode) == 1

    /**
     * 이미지 INSERT 와 세션 COMPLETED 를 함께 커밋한다. 세션 조건이 맞지 않으면 INSERT 까지 되돌린다.
     * 먼저 세션 행을 잠그고 조건을 확인해, 이미 완료됐거나 lease 를 잃은 요청이 같은 확정 키로 INSERT 하다 UNIQUE 위반을 내지 않게 한다.
     */
    @Transactional
    override fun complete(
        uploadId: String,
        token: String,
        image: Image,
    ): Image? {
        imageUploadJpaRepository.findCompletableForUpdate(uploadId, token) ?: return null
        val saved = imageJpaRepository.save(ImageEntity.from(image))
        if (imageUploadJpaRepository.markCompleted(uploadId, token, saved.id) != 1) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
            return null
        }
        return saved.toDomain()
    }

    override fun clearParId(uploadId: String): Boolean = imageUploadJpaRepository.clearParId(uploadId) == 1

    override fun expire(
        uploadId: String,
        now: Instant,
    ): Boolean = imageUploadJpaRepository.expire(uploadId, now) == 1

    override fun deleteTerminal(uploadId: String): Boolean = imageUploadJpaRepository.deleteTerminal(uploadId) == 1

    override fun findStale(
        createdBefore: Instant,
        limit: Int,
    ): List<ImageUpload> =
        imageUploadJpaRepository.findStale(createdBefore, PageRequest.of(0, limit)).map { it.toDomain() }
}
