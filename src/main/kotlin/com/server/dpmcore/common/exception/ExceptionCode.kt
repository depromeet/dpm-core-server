package com.server.dpmcore.common.exception

import org.springframework.http.HttpStatus

interface ExceptionCode {
    fun getStatus(): HttpStatus

    fun getCode(): String

    fun getMessage(): String
}
