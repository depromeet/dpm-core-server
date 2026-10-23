package core.application.image.infrastructure

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

interface R2ClientProvider {
    /** 클라이언트를 만들 수 없으면(설정 누락) ImageStorageUnavailableException 을 던진다. */
    fun client(): S3Client

    fun presigner(): S3Presigner
}
