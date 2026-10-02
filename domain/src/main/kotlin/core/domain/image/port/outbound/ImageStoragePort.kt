package core.domain.image.port.outbound

/**
 * 이미지 원본 바이트 저장소. 이미지는 10MiB 이하라 바이트 배열로 주고받는다.
 */
interface ImageStoragePort {
    fun put(
        objectKey: String,
        content: ByteArray,
        contentType: String,
    )

    /** [maxBytes] 를 넘는 객체는 끝까지 읽지 않고 실패한다. */
    fun get(
        objectKey: String,
        maxBytes: Long,
    ): ByteArray

    fun delete(objectKey: String)
}
