package com.server.dpmcore.session.presentation.controller

import com.server.dpmcore.cohort.domain.model.CohortId
import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.session.application.SessionReadService
import com.server.dpmcore.session.domain.model.SessionId
import com.server.dpmcore.session.presentation.dto.response.NextSessionResponse
import com.server.dpmcore.session.presentation.dto.response.SessionDetailResponse
import com.server.dpmcore.session.presentation.dto.response.SessionListResponse
import com.server.dpmcore.session.presentation.mapper.SessionMapper
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/sessions")
class SessionController(
    private val sessionReadService: SessionReadService,
) : SessionApi {
    @GetMapping("/next")
    override fun getNextSession(): CustomResponse<NextSessionResponse> {
        val nextSession = sessionReadService.getNextSession()

        return CustomResponse.ok(nextSession?.let { SessionMapper.toNextSessionResponse(it) })
    }

    @GetMapping
    override fun getAllSessions(
        @RequestParam(name = "cohortId") cohortId: CohortId,
    ): CustomResponse<SessionListResponse> {
        val sessions = sessionReadService.getAllSessions(cohortId)

        return CustomResponse.ok(SessionMapper.toSessionListResponse(sessions))
    }

    @GetMapping("/{sessionId}")
    override fun getSessionById(
        @PathVariable(name = "sessionId") sessionId: SessionId,
    ): CustomResponse<SessionDetailResponse> {
        val session = sessionReadService.getSessionById(sessionId)

        return CustomResponse.ok(session.let { SessionMapper.toSessionDetailResponse(it) })
    }
}
