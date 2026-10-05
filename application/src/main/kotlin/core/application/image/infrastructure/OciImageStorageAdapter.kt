package core.application.image.infrastructure

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.model.Range
import com.oracle.bmc.objectstorage.model.CopyObjectDetails
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails
import com.oracle.bmc.objectstorage.model.PreauthenticatedRequest
import com.oracle.bmc.objectstorage.model.WorkRequest
import com.oracle.bmc.objectstorage.requests.CopyObjectRequest
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest
import com.oracle.bmc.objectstorage.requests.DeletePreauthenticatedRequestRequest
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.GetWorkRequestRequest
import com.oracle.bmc.objectstorage.requests.HeadObjectRequest
import core.application.image.application.exception.ImageStorageUnavailableException
import core.application.image.application.properties.ImageStorageProperties
import core.domain.image.port.outbound.CopyStart
import core.domain.image.port.outbound.CopyStatus
import core.domain.image.port.outbound.DownloadedObject
import core.domain.image.port.outbound.ImageStoragePort
import core.domain.image.port.outbound.PreauthenticatedUrl
import core.domain.image.port.outbound.StoredObject
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.Date
import java.util.UUID

/**
 * 비공개 버킷에 대한 PAR 발급/회수, 검증용 내려받기, 확정 키 복사. 오류 상세(버킷, 키, SDK 메시지)는 로그에만 남기고 503 으로 낸다.
 * PAR URL 은 권한 그 자체라 로그에 남기지 않는다(키, parId, opc-request-id 만 남긴다).
 */
@Component
class OciImageStorageAdapter(
    private val properties: ImageStorageProperties,
    private val clientProvider: ObjectStorageClientProvider,
) : ImageStoragePort {
    private val logger = KotlinLogging.logger { }

    override fun createUploadUrl(
        objectKey: String,
        expiresAt: Instant,
    ): PreauthenticatedUrl =
        createPar(objectKey, expiresAt, CreatePreauthenticatedRequestDetails.AccessType.ObjectWrite)

    override fun createReadUrl(
        objectKey: String,
        expiresAt: Instant,
    ): PreauthenticatedUrl = createPar(objectKey, expiresAt, CreatePreauthenticatedRequestDetails.AccessType.ObjectRead)

    override fun revokeUrl(parId: String) {
        call("revoke-par", "parId=$parId", notFound = { }) {
            clientProvider.get().deletePreauthenticatedRequest(
                DeletePreauthenticatedRequestRequest
                    .builder()
                    .namespaceName(properties.namespace)
                    .bucketName(properties.bucket)
                    .parId(parId)
                    .build(),
            )
        }
    }

    override fun download(
        objectKey: String,
        maxBytes: Long,
        target: Path,
    ): DownloadedObject? =
        call("download", objectKey, notFound = { null }) {
            try {
                downloadRange(objectKey, maxBytes, target)
            } catch (e: BmcException) {
                if (e.statusCode != RANGE_NOT_SATISFIABLE) throw e
                downloadEmpty(objectKey, target)
            }
        }

    /**
     * 0..maxBytes(포함) 범위만 요청해 상한 + 1 바이트까지만 전송받는다. 범위 없이 받으면 큰 객체를 닫을 때
     * HTTP 클라이언트가 남은 본문을 끝까지 읽어 버리므로(graceful close) 전송량을 요청에서 제한한다.
     */
    private fun downloadRange(
        objectKey: String,
        maxBytes: Long,
        target: Path,
    ): DownloadedObject {
        val response =
            clientProvider.get().getObject(
                GetObjectRequest
                    .builder()
                    .namespaceName(properties.namespace)
                    .bucketName(properties.bucket)
                    .objectName(objectKey)
                    .range(Range(0L, maxBytes))
                    .build(),
            )
        val etag = checkNotNull(response.eTag) { "ETag 가 없습니다" }
        val input = response.inputStream ?: throw IllegalStateException("응답 본문이 없습니다")
        val size = input.use { Files.newOutputStream(target).use { output -> it.copyAtMost(output, maxBytes + 1) } }
        return DownloadedObject(etag, size, response.contentType, response.contentEncoding)
    }

    /** 범위 요청이 416 이면 빈 객체일 때뿐이다. HEAD 로 길이 0 을 확인한 경우에만 빈 파일로 보고, 아니면 실패로 던진다. */
    private fun downloadEmpty(
        objectKey: String,
        target: Path,
    ): DownloadedObject {
        val response =
            clientProvider.get().headObject(
                HeadObjectRequest
                    .builder()
                    .namespaceName(properties.namespace)
                    .bucketName(properties.bucket)
                    .objectName(objectKey)
                    .build(),
            )
        check(response.contentLength == 0L) { "범위 요청이 416 인데 객체 길이가 0 이 아닙니다: ${response.contentLength}" }
        Files.newOutputStream(target).close()
        return DownloadedObject(
            checkNotNull(response.eTag) { "ETag 가 없습니다" },
            0L,
            response.contentType,
            response.contentEncoding,
        )
    }

    override fun head(objectKey: String): StoredObject? =
        call("head", objectKey, notFound = { null }) {
            val response =
                clientProvider.get().headObject(
                    HeadObjectRequest
                        .builder()
                        .namespaceName(properties.namespace)
                        .bucketName(properties.bucket)
                        .objectName(objectKey)
                        .build(),
                )
            StoredObject(checkNotNull(response.eTag) { "ETag 가 없습니다" }, checkNotNull(response.contentLength))
        }

    override fun startCopy(
        sourceKey: String,
        sourceEtag: String,
        destinationKey: String,
    ): CopyStart =
        try {
            call("copy", "$sourceKey -> $destinationKey", rethrowPrecondition = true) {
                val response =
                    clientProvider.get().copyObject(
                        CopyObjectRequest
                            .builder()
                            .namespaceName(properties.namespace)
                            .bucketName(properties.bucket)
                            .copyObjectDetails(
                                CopyObjectDetails
                                    .builder()
                                    .sourceObjectName(sourceKey)
                                    // 검증한 바이트와 같은 객체일 때만, 확정 키가 비어 있을 때만 복사한다.
                                    .sourceObjectIfMatchETag(sourceEtag)
                                    .destinationRegion(properties.region)
                                    .destinationNamespace(properties.namespace)
                                    .destinationBucket(properties.bucket)
                                    .destinationObjectName(destinationKey)
                                    .destinationObjectIfNoneMatchETag("*")
                                    .build(),
                            ).build(),
                    )
                CopyStart.Started(checkNotNull(response.opcWorkRequestId) { "work request id 가 없습니다" })
            }
        } catch (e: PreconditionRejected) {
            CopyStart.PreconditionFailed
        }

    // 권한이 없을 때도 OCI 는 404 를 준다. 실패로 단정하지 않고 오류 로그와 503(재시도)으로 낸다.
    override fun copyStatus(workRequestId: String): CopyStatus =
        call("copy-status", "workRequestId=$workRequestId") {
            val status =
                clientProvider
                    .get()
                    .getWorkRequest(GetWorkRequestRequest.builder().workRequestId(workRequestId).build())
                    .workRequest
                    ?.status
            when (status) {
                WorkRequest.Status.Completed -> CopyStatus.COMPLETED
                WorkRequest.Status.Failed, WorkRequest.Status.Canceled -> CopyStatus.FAILED
                // 모르는 값은 진행 중으로 둔다. 정리 작업이 확정 키를 지우지 않게 하기 위해서다.
                else -> CopyStatus.IN_PROGRESS
            }
        }

    override fun delete(objectKey: String) {
        call("delete", objectKey, notFound = { }) {
            clientProvider.get().deleteObject(
                DeleteObjectRequest
                    .builder()
                    .namespaceName(properties.namespace)
                    .bucketName(properties.bucket)
                    .objectName(objectKey)
                    .build(),
            )
        }
    }

    private fun createPar(
        objectKey: String,
        expiresAt: Instant,
        accessType: CreatePreauthenticatedRequestDetails.AccessType,
    ): PreauthenticatedUrl =
        call("create-par", objectKey) {
            val client = clientProvider.get()
            val par =
                client
                    .createPreauthenticatedRequest(
                        CreatePreauthenticatedRequestRequest
                            .builder()
                            .namespaceName(properties.namespace)
                            .bucketName(properties.bucket)
                            .createPreauthenticatedRequestDetails(
                                CreatePreauthenticatedRequestDetails
                                    .builder()
                                    // 이름은 PAR 목록에서 구분하는 용도다. 회원 정보는 넣지 않는다.
                                    .name("${accessType.value}-${UUID.randomUUID()}")
                                    .objectName(objectKey)
                                    .accessType(accessType)
                                    .bucketListingAction(PreauthenticatedRequest.BucketListingAction.Deny)
                                    .timeExpires(Date.from(expiresAt))
                                    .build(),
                            ).build(),
                    ).preauthenticatedRequest
            val url = par.fullPath?.takeIf { it.isNotBlank() } ?: (client.endpoint.trimEnd('/') + par.accessUri)
            PreauthenticatedUrl(
                parId = checkNotNull(par.id) { "PAR id 가 없습니다" },
                url = url,
                expiresAt = par.timeExpires?.toInstant() ?: expiresAt,
            )
        }

    private fun <T> call(
        operation: String,
        target: String,
        notFound: (() -> T)? = null,
        rethrowPrecondition: Boolean = false,
        block: () -> T,
    ): T {
        return try {
            block()
        } catch (e: ImageStorageUnavailableException) {
            throw e
        } catch (e: BmcException) {
            if (e.statusCode == NOT_FOUND && notFound != null) return notFound()
            if (rethrowPrecondition && e.statusCode in PRECONDITION_STATUSES) {
                logger.info {
                    "OCI Object Storage $operation 조건 불일치: $target, " +
                        "status=${e.statusCode}, opcRequestId=${e.opcRequestId}"
                }
                throw PreconditionRejected()
            }
            // 버킷 권한이 없을 때도 OCI 는 404 를 주므로 상태 코드와 요청 id 를 남긴다.
            logger.error {
                "OCI Object Storage $operation 실패: $target, status=${e.statusCode}, " +
                    "serviceCode=${e.serviceCode}, timeout=${e.isTimeout}, opcRequestId=${e.opcRequestId}"
            }
            throw ImageStorageUnavailableException()
        } catch (e: Exception) {
            logger.error(e) { "OCI Object Storage $operation 실패: $target" }
            throw ImageStorageUnavailableException()
        }
    }

    /** [limit] 바이트까지만 옮기고 옮긴 수를 돌려준다. */
    private fun InputStream.copyAtMost(
        output: OutputStream,
        limit: Long,
    ): Long {
        val buffer = ByteArray(BUFFER_SIZE)
        var copied = 0L
        while (copied < limit) {
            val read = read(buffer, 0, minOf(buffer.size.toLong(), limit - copied).toInt())
            if (read < 0) break
            output.write(buffer, 0, read)
            copied += read
        }
        return copied
    }

    private class PreconditionRejected : RuntimeException()

    companion object {
        private const val NOT_FOUND = 404
        private const val RANGE_NOT_SATISFIABLE = 416
        private const val BUFFER_SIZE = 64 * 1024

        // 원본 ETag 불일치(412), 대상 존재(409/412)만 복사 거절로 본다. 404 는 권한 없음일 수 있어 503 으로 낸다.
        private val PRECONDITION_STATUSES = setOf(409, 412)
    }
}
