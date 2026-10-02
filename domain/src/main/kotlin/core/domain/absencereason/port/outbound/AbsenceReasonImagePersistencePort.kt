package core.domain.absencereason.port.outbound

import core.domain.image.vo.ImageId

/** 결석 사유서 1건에 이미지 N장(순서 있음). 이미지 하나는 동시에 한 사유서에만 붙는다(UNIQUE image_id). */
interface AbsenceReasonImagePersistencePort {
    /** 표시 순서대로 */
    fun findImageIds(absenceReasonId: Long): List<ImageId>

    /** 사유서 id 별 표시 순서대로. 첨부가 없는 사유서는 키가 없다. */
    fun findImageIdsByAbsenceReasonIds(absenceReasonIds: List<Long>): Map<Long, List<ImageId>>

    /** 이미 어딘가에 붙어 있는 이미지만 image id -> absence reason id */
    fun findAbsenceReasonIdsByImageIds(imageIds: List<ImageId>): Map<ImageId, Long>

    fun exists(
        absenceReasonId: Long,
        imageId: ImageId,
    ): Boolean

    /** 기존 링크를 지우고 [imageIds] 순서로 다시 건다. 호출자가 세션 쓰기 잠금을 잡은 트랜잭션이어야 한다. */
    fun replaceImages(
        absenceReasonId: Long,
        imageIds: List<ImageId>,
    )

    fun deleteAll(absenceReasonId: Long)

    companion object {
        /** 동시 첨부 충돌을 409 로 바꿀 때 오류 메시지에서 찾는 제약 이름 */
        const val IMAGE_UNIQUE_CONSTRAINT = "uk_absence_reason_images_image_id"
    }
}
