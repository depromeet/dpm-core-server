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
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

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

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected fun handleException(exception: Exception): CustomResponse<Void> {
        logger.error { "Exception: ${exception.javaClass.simpleName} - ${exception.message}" }
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
