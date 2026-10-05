package core.domain.image.port.outbound

import core.domain.image.aggregate.Image
import core.domain.image.aggregate.ImageUpload
import java.time.Instant

/**
 * 업로드 세션 저장소. 상태 변경은 모두 조건부 UPDATE 이며 반영된 경우에만 true 를 돌려준다.
 * token 조건이 붙은 변경은 lease 를 잃은(만료 후 다른 요청이 가져간) 처리자가 결과를 덮어쓰지 못하게 한다.
 */
interface ImageUploadPersistencePort {
    fun save(upload: ImageUpload): ImageUpload

    fun findById(uploadId: String): ImageUpload?

    /** PENDING 또는 lease 가 끝난 VERIFYING 을, 업로드 URL 이 아직 유효할 때만 VERIFYING 으로 가져온다. */
    fun startVerification(
        uploadId: String,
        token: String,
        now: Instant,
        leaseUntil: Instant,
    ): Boolean

    /** 업로드된 객체가 아직 없을 때 VERIFYING → PENDING. */
    fun releaseVerification(
        uploadId: String,
        token: String,
    ): Boolean

    /** VERIFYING → COPYING. 같은 token 으로 복사까지 이어서 처리한다. */
    fun markValidated(
        uploadId: String,
        token: String,
        etag: String,
        leaseUntil: Instant,
    ): Boolean

    fun reject(
        uploadId: String,
        token: String,
        failureCode: String,
    ): Boolean

    /** lease 가 비어 있거나 끝난 COPYING 을 가져온다. */
    fun acquireCopy(
        uploadId: String,
        token: String,
        now: Instant,
        leaseUntil: Instant,
    ): Boolean

    fun recordCopyWorkRequest(
        uploadId: String,
        token: String,
        workRequestId: String,
    ): Boolean

    /** COPYING 의 lease 만 비운다(상태 유지). */
    fun releaseCopy(
        uploadId: String,
        token: String,
    ): Boolean

    fun fail(
        uploadId: String,
        token: String,
        failureCode: String,
    ): Boolean

    /**
     * 한 트랜잭션에서 이미지 행을 넣고 COPYING → COMPLETED 로 바꾼다. token 이 맞지 않으면 롤백하고 null.
     */
    fun complete(
        uploadId: String,
        token: String,
        image: Image,
    ): Image?

    /** COMPLETED 의 PAR 회수·업로드 객체 삭제가 끝났음을 표시한다(parId = null). */
    fun clearParId(uploadId: String): Boolean

    /** lease 가 없는 PENDING/VERIFYING 을 EXPIRED 로 바꾼다. */
    fun expire(
        uploadId: String,
        now: Instant,
    ): Boolean

    /** REJECTED/FAILED/EXPIRED 행만 지운다. */
    fun deleteTerminal(uploadId: String): Boolean

    /** 정리할 일이 남은(parId 가 남은) 오래된 세션. COMPLETED 이면서 정리가 끝난 행은 포함하지 않는다. */
    fun findStale(
        createdBefore: Instant,
        limit: Int,
    ): List<ImageUpload>
}
