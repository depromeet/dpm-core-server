package com.server.dpmcore.common.exception

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.stream.Collectors

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

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    protected fun handleMethodArgumentNotValidException(
        exception: MethodArgumentNotValidException,
    ): CustomResponse<Void> {
        val message =
            if (exception.bindingResult.fieldErrors.isNotEmpty()) {
                exception.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
            } else {
                "Invalid input parameters"
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
        logger.error("HttpMessageNotReadableException: {}", exception)

        val exceptionCode: ExceptionCode =
            when (val cause = exception.cause) {
                is MismatchedInputException -> {
                    val fieldName: String =
                        cause.path
                            .stream()
                            .map { it.fieldName }
                            .collect(Collectors.joining("."))

                    MismatchedInputExceptionCode(fieldName = fieldName)
                }

                else -> JsonParseException()
            }

        return CustomResponse.error(exceptionCode)
    }
}
