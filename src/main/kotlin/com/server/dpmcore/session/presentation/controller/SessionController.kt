package com.server.dpmcore.session.presentation.controller

import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.session.application.SessionReadService
import com.server.dpmcore.session.presentation.dto.response.NextSessionResponse
import com.server.dpmcore.session.presentation.mapper.SessionMapper
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/sessions")
class SessionController(
    private val sessionReadService: SessionReadService,
) {
    @GetMapping("/next")
    fun getNextSession(): CustomResponse<NextSessionResponse> {
        val nextSession = sessionReadService.getNextSession()

        return CustomResponse.ok(nextSession?.let { SessionMapper.toNextSessionResponse(it) })
    }
}
