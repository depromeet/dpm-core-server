package com.server.dpmcore.common.exception

import jakarta.servlet.http.HttpServletResponse
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.springframework.stereotype.Component

@Component
class CustomResponseStatusAspect(
    private val response: HttpServletResponse,
) {
    @Around("within(* com.server.dpmcore..*Controller.*(..))")
    @Throws(Throwable::class)
    fun handleResponseStatus(joinPoint: ProceedingJoinPoint): Any? {
        val result = joinPoint.proceed()

        if (result is CustomResponse<*>) {
            response.status = result.status.value()
        }

        return result
    }
}
