package core.application.attendance.presentation.request

import core.application.attendance.application.exception.InvalidAbsenceReasonImageException
import core.domain.image.vo.ImageId

const val ABSENCE_REASON_IMAGE_IDS_DESCRIPTION =
    "첨부 이미지 id (POST /v3/images/uploads/{uploadId}/complete 로 받은 본인 이미지). 생략/null: 기존 첨부 유지(새 사유서는 첨부 없음), " +
        "[]: 모두 해제, 값: 이 순서로 교체. 중복 불가, 다른 사유서에 붙은 이미지는 409"

/** JSON 배열 안의 null 은 Kotlin 타입으로 막히지 않으므로 여기서 400 으로 거절한다. 양수·중복 검사는 서비스가 한다. */
internal fun toAbsenceReasonImageIds(imageIds: List<Long?>): List<ImageId> =
    imageIds.map { ImageId(it ?: throw InvalidAbsenceReasonImageException()) }
