package core.application.attendance.application.exception

import core.application.common.exception.BusinessException
import core.application.common.exception.ExceptionCode
import core.domain.absencereason.port.outbound.AbsenceReasonImagePersistencePort
import org.springframework.dao.DataIntegrityViolationException

class AbsenceReasonImageAlreadyAttachedException(
    code: ExceptionCode = AttendanceExceptionCode.ABSENCE_REASON_IMAGE_ALREADY_ATTACHED,
) : BusinessException(code)

/** 동시 첨부의 UNIQUE(image_id) 위반만 409 로 바꾼다. 롤백된 트랜잭션 밖(컨트롤러)에서 감싼다. */
inline fun <T> translateAbsenceReasonImageConflict(block: () -> T): T =
    try {
        block()
    } catch (e: DataIntegrityViolationException) {
        if (isImageUniqueViolation(e)) throw AbsenceReasonImageAlreadyAttachedException()
        throw e
    }

fun isImageUniqueViolation(e: DataIntegrityViolationException): Boolean =
    generateSequence<Throwable>(e) { it.cause }.any {
        it.message?.contains(AbsenceReasonImagePersistencePort.IMAGE_UNIQUE_CONSTRAINT, ignoreCase = true) == true
    }
