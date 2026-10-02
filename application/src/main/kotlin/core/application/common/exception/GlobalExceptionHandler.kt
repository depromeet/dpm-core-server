package core.application.common.exception

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.InvalidNullException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import core.application.security.oauth.exception.InvalidAccessTokenException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = KotlinLogging.logger { GlobalExceptionHandler::class.java }

    @ExceptionHandler(BusinessException::class)
    protected fun handleBusinessException(
        exception: BusinessException,
        response: HttpServletResponse,
    ): CustomResponse<Void> {
        response.status = exception.getCode().getStatus().value()

        logger.error {
            "${exception.getCode()} Exception ${
                exception.getCode().getCode()
            }: ${exception.getCode().getMessage()}"
        }

        return CustomResponse.error(exception.getCode())
    }

    @ExceptionHandler(InvalidAccessTokenException::class)
    protected fun handleInvalidAccessTokenException(
        exception: InvalidAccessTokenException,
        response: HttpServletResponse,
    ): CustomResponse<Void> {
        response.status = exception.getCode().getStatus().value()

        logger.error {
            "${exception.getCode()} Exception ${
                exception.getCode().getCode()
            }: ${exception.getCode().getMessage()}"
        }

        return CustomResponse.error(exception.getCode())
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    protected fun handleMethodArgumentNotValidException(
        exception: MethodArgumentNotValidException,
    ): CustomResponse<Void> {
        val message =
            if (exception.bindingResult.fieldErrors.isNotEmpty()) {
                exception.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
            } else {
                GlobalExceptionCode.INVALID_INPUT.message
            }
        return CustomResponse.error(GlobalExceptionCode.INVALID_INPUT, message)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    protected fun handleNoResourceFoundException(exception: NoResourceFoundException): CustomResponse<Void> =
        CustomResponse.error(GlobalExceptionCode.NOT_FOUND)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    protected fun handleHttpRequestMethodNotSupportedException(
        exception: HttpRequestMethodNotSupportedException,
    ): CustomResponse<Void> {
        logger.error { "Exception: ${exception.javaClass.simpleName} - ${exception.message}" }
        return CustomResponse.error(GlobalExceptionCode.METHOD_NOT_ALLOWED)
    }

    // 멀티파트 크기 초과는 컨트롤러 진입 전(파싱 단계)에 나므로 Exception 핸들러의 500 으로 떨어지지 않게 따로 받는다.
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    fun handleMaxUploadSizeExceededException(exception: MaxUploadSizeExceededException): CustomResponse<Void> =
        CustomResponse.error(GlobalExceptionCode.PAYLOAD_TOO_LARGE)

    @ExceptionHandler(MissingServletRequestPartException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMissingServletRequestPartException(exception: MissingServletRequestPartException): CustomResponse<Void> =
        CustomResponse.error(GlobalExceptionCode.INVALID_INPUT, "${exception.requestPartName}: 필수 입력값입니다")

    @ExceptionHandler(MultipartException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMultipartException(exception: MultipartException): CustomResponse<Void> {
        // 임시 디렉터리 오류 같은 서버 측 파싱 실패도 여기로 오므로 원인을 남긴다.
        logger.warn(exception) { "Multipart 요청을 처리하지 못했습니다: ${exception.message}" }
        return CustomResponse.error(GlobalExceptionCode.INVALID_INPUT, "올바른 multipart/form-data 요청이 아닙니다")
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected fun handleException(exception: Exception): CustomResponse<Void> {
        // 예상치 못한 500 은 스택트레이스가 없으면 사후 추적이 불가능하다. 레벨은 그대로 두고 예외만 함께 넘긴다.
        logger.error(exception) { "Exception: ${exception.javaClass.simpleName} - ${exception.message}" }
        return CustomResponse.error(GlobalExceptionCode.SERVER_ERROR)
    }

    @ExceptionHandler(AuthorizationDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    protected fun handleAuthorizationDeniedException(exception: AuthorizationDeniedException): CustomResponse<Void> =
        CustomResponse.error(GlobalExceptionCode.ACCESS_DENIED)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleHttpMessageNotReadableException(exception: HttpMessageNotReadableException): CustomResponse<Void> {
        val message =
            when (val cause = exception.cause) {
                is InvalidNullException -> handleInvalidNullException(cause)
                is InvalidFormatException -> handleInvalidFormat(cause)
                is MismatchedInputException -> handleMismatchedInput(cause)
                else -> GlobalExceptionCode.INVALID_INPUT.message
            }

        return CustomResponse.error(GlobalExceptionCode.INVALID_INPUT, message)
    }

    private fun handleInvalidNullException(e: InvalidNullException): String {
        val name = e.path.lastOrNull()?.fieldName ?: "알 수 없는 필드"
        return "$name: 필수 입력값입니다"
    }

    private fun handleInvalidFormat(e: InvalidFormatException): String =
        "${e.path.last().fieldName.orEmpty()}: 올바른 형식이어야 합니다"

    private fun handleMismatchedInput(e: MismatchedInputException): String =
        "${e.path.last().fieldName.orEmpty()}: 필드 값이 누락되었거나 타입이 맞지 않습니다"
}
