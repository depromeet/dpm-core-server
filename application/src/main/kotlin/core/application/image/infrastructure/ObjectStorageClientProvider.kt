package core.application.image.infrastructure

import com.oracle.bmc.objectstorage.ObjectStorage

fun interface ObjectStorageClientProvider {
    /** 클라이언트를 만들 수 없으면(설정 누락, 인증 실패) ImageStorageUnavailableException 을 던진다. */
    fun get(): ObjectStorage
}
