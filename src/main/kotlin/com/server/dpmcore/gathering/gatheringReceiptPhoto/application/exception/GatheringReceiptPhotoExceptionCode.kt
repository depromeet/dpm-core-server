package com.server.dpmcore.gathering.gatheringReceiptPhoto.application.exception

import com.server.dpmcore.common.exception.ExceptionCode
import org.springframework.http.HttpStatus

enum class GatheringReceiptPhotoExceptionCode(
    @JvmField val status: HttpStatus,
    @JvmField val code: String,
    @JvmField val message: String,
) : ExceptionCode {
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "RECEIPT_PHOTO-500-01", "예상치 못한 서버 에러가 발생했습니다"),

    GATHERING_RECEIPT_PHOTO_NOT_FOUND(HttpStatus.BAD_REQUEST, "RECEIPT_PHOTO-404-01", "존재하지 않는 회식 영수증 사진입니다."),
    GATHERING_RECEIPT_PHOTO_ID_REQUIRED(HttpStatus.BAD_REQUEST, "RECEIPT_PHOTO-404-02", "회식 영수증 사진 ID는 null일 수 없습니다."),
    ;

    override fun getStatus(): HttpStatus = status

    override fun getCode(): String = code

    override fun getMessage(): String = message
}
