package core.application.image.infrastructure

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.requests.DeleteObjectRequest
import com.oracle.bmc.objectstorage.requests.GetObjectRequest
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import core.application.image.application.exception.ImageNotFoundException
import core.application.image.application.exception.ImageStorageUnavailableException
import core.application.image.application.properties.ImageStorageProperties
import core.domain.image.port.outbound.ImageStoragePort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream

/**
 * 비공개 버킷에 대한 put/get/delete. 오류 상세(버킷, 키, SDK 메시지)는 로그에만 남기고 응답은 404/503 으로만 낸다.
 * 본문을 바이트 배열로 넘겨 SDK 가 인증 갱신 후 재전송할 때도 스트림을 다시 읽을 수 있게 한다.
 */
@Component
class OciImageStorageAdapter(
    private val properties: ImageStorageProperties,
    private val clientProvider: ObjectStorageClientProvider,
) : ImageStoragePort {
    private val logger = KotlinLogging.logger { }

    override fun put(
        objectKey: String,
        content: ByteArray,
        contentType: String,
    ) {
        call("put", objectKey) {
            clientProvider.get().putObject(
                PutObjectRequest
                    .builder()
                    .namespaceName(properties.namespace)
                    .bucketName(properties.bucket)
                    .objectName(objectKey)
                    .contentLength(content.size.toLong())
                    .contentType(contentType)
                    // 새 UUID 키라 덮어쓸 일이 없다. 덮어쓰기 권한(OBJECT_OVERWRITE)도 필요 없게 한다.
                    .ifNoneMatch("*")
                    .putObjectBody(ByteArrayInputStream(content))
                    .build(),
            )
        }
    }

    override fun get(
        objectKey: String,
        maxBytes: Long,
    ): ByteArray =
        call("get", objectKey) {
            val response =
                clientProvider.get().getObject(
                    GetObjectRequest
                        .builder()
                        .namespaceName(properties.namespace)
                        .bucketName(properties.bucket)
                        .objectName(objectKey)
                        .build(),
                )
            val input = response.inputStream ?: throw IllegalStateException("응답 본문이 없습니다")
            input.use {
                val declaredLength = response.contentLength
                check(declaredLength == null || declaredLength <= maxBytes) { "객체가 상한보다 큽니다: $declaredLength" }
                val bytes = it.readNBytes(Math.toIntExact(maxBytes + 1))
                check(bytes.size <= maxBytes) { "객체가 상한보다 큽니다" }
                bytes
            }
        }

    override fun delete(objectKey: String) {
        try {
            call("delete", objectKey) {
                clientProvider.get().deleteObject(
                    DeleteObjectRequest
                        .builder()
                        .namespaceName(properties.namespace)
                        .bucketName(properties.bucket)
                        .objectName(objectKey)
                        .build(),
                )
            }
        } catch (e: ImageNotFoundException) {
            // 이미 없으면 지울 것도 없다.
        }
    }

    private fun <T> call(
        operation: String,
        objectKey: String,
        block: () -> T,
    ): T =
        try {
            block()
        } catch (e: ImageStorageUnavailableException) {
            throw e
        } catch (e: BmcException) {
            logger.error {
                "OCI Object Storage $operation 실패: key=$objectKey, status=${e.statusCode}, " +
                    "serviceCode=${e.serviceCode}, timeout=${e.isTimeout}, opcRequestId=${e.opcRequestId}"
            }
            // 버킷 권한이 없을 때도 OCI 는 404 를 주므로 로그는 남긴다. 업로드의 404 는 설정/권한 문제라 503 이다.
            if (e.statusCode == NOT_FOUND && operation != "put") throw ImageNotFoundException()
            throw ImageStorageUnavailableException()
        } catch (e: Exception) {
            logger.error(e) { "OCI Object Storage $operation 실패: key=$objectKey" }
            throw ImageStorageUnavailableException()
        }

    companion object {
        private const val NOT_FOUND = 404
    }
}
