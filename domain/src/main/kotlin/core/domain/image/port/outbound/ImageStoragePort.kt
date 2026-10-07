package core.domain.image.port.outbound

import java.nio.file.Path
import java.time.Instant

/**
 * 이미지 원본 저장소(비공개 버킷). 바이트는 서버를 거치지 않고 PAR URL 로 프론트와 저장소가 직접 주고받는다.
 * 서버는 검증할 때만 상한 + 1 바이트까지 임시 파일로 내려받는다.
 */
interface ImageStoragePort {
    /** [objectKey] 한 객체에만 쓸 수 있는 URL. */
    fun createUploadUrl(
        objectKey: String,
        expiresAt: Instant,
    ): PreauthenticatedUrl

    /** [objectKey] 한 객체만 읽을 수 있는 URL. */
    fun createReadUrl(
        objectKey: String,
        expiresAt: Instant,
    ): PreauthenticatedUrl

    /**
     * [objectKey] 한 객체만 읽을 수 있고, 브라우저가 열면 [fileName] 으로 저장하는 URL(Content-Disposition: attachment).
     * [fileName] 이 없으면 저장 이름은 브라우저가 정한다.
     */
    fun createDownloadUrl(
        objectKey: String,
        expiresAt: Instant,
        fileName: String?,
    ): PreauthenticatedUrl

    /** 이미 없거나 만료로 지워진 PAR 은 성공으로 본다. */
    fun revokeUrl(parId: String)

    /**
     * 객체를 [target] 에 최대 [maxBytes] + 1 바이트까지 쓴다(그 이상은 전송받지 않는다). 객체가 없으면 null.
     * 돌려준 크기가 [maxBytes] 를 넘으면 상한 초과다.
     */
    fun download(
        objectKey: String,
        maxBytes: Long,
        target: Path,
    ): DownloadedObject?

    fun head(objectKey: String): StoredObject?

    /**
     * 원본 ETag 가 [sourceEtag] 일 때만, 대상이 없을 때만 복사한다(비동기 work request).
     * 응답을 받지 못한 실패는 예외로 던진다. 이 경우 복사가 시작됐는지 알 수 없다.
     */
    fun startCopy(
        sourceKey: String,
        sourceEtag: String,
        destinationKey: String,
    ): CopyStart

    fun copyStatus(workRequestId: String): CopyStatus

    /** 이미 없으면 성공으로 본다. */
    fun delete(objectKey: String)
}

/** URL 자체가 권한이다. 로그와 toString 에 남기지 않는다. */
class PreauthenticatedUrl(
    val parId: String,
    val url: String,
    val expiresAt: Instant,
) {
    override fun toString(): String = "PreauthenticatedUrl(expiresAt=$expiresAt)"
}

data class StoredObject(
    val etag: String,
    val size: Long,
)

data class DownloadedObject(
    val etag: String,
    /** 내려받은 바이트 수. 상한을 넘으면 상한 + 1 이거나 길이 헤더 값이다. */
    val size: Long,
    val contentType: String?,
    val contentEncoding: String?,
)

sealed interface CopyStart {
    data class Started(
        val workRequestId: String,
    ) : CopyStart

    /** 원본 ETag 불일치·원본 없음·대상 이미 있음 중 하나로 저장소가 거절했다. */
    data object PreconditionFailed : CopyStart
}

enum class CopyStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
}
