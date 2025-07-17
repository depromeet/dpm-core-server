package com.server.dpmcore.session.presentation.controller

import com.server.dpmcore.cohort.domain.model.CohortId
import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.session.application.SessionQueryService
import com.server.dpmcore.session.domain.model.SessionId
import com.server.dpmcore.session.presentation.dto.response.AttendanceTimeResponse
import com.server.dpmcore.session.presentation.dto.response.NextSessionResponse
import com.server.dpmcore.session.presentation.dto.response.SessionDetailResponse
import com.server.dpmcore.session.presentation.dto.response.SessionListResponse
import com.server.dpmcore.session.presentation.dto.response.SessionWeeksResponse
import com.server.dpmcore.session.presentation.mapper.SessionMapper
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/sessions")
class SessionQueryController(
    private val sessionQueryService: SessionQueryService,
) : SessionQueryApi {
    @GetMapping("/next")
    override fun getNextSession(): CustomResponse<NextSessionResponse> {
        val response =
            sessionQueryService
                .getNextSession()
                ?.let { SessionMapper.toNextSessionResponse(it) }

        return CustomResponse.ok(response)
    }

    @GetMapping
    override fun getAllSessions(
        @RequestParam(name = "cohortId") cohortId: CohortId,
    ): CustomResponse<SessionListResponse> {
        val response =
            sessionQueryService
                .getAllSessions(cohortId)
                .let { SessionMapper.toSessionListResponse(it) }

        return CustomResponse.ok(response)
    }

    @GetMapping("/{sessionId}")
    override fun getSessionById(
        @PathVariable(name = "sessionId") sessionId: SessionId,
    ): CustomResponse<SessionDetailResponse> {
        val response =
            sessionQueryService
                .getSessionById(sessionId)
                .let { SessionMapper.toSessionDetailResponse(it) }

        return CustomResponse.ok(response)
    }

    @GetMapping("/{sessionId}/attendance-time")
    override fun getAttendanceTime(
        @PathVariable(name = "sessionId") sessionId: SessionId,
    ): CustomResponse<AttendanceTimeResponse> {
        val response =
            sessionQueryService
                .getAttendanceTime(sessionId)
                .let { SessionMapper.toAttendanceTimeResponse(it) }

        return CustomResponse.ok(response)
    }

    @GetMapping("/weeks")
    override fun getSessionWeeks(): CustomResponse<SessionWeeksResponse> {
        val response =
            sessionQueryService
                .getSessionWeeks()
                .let { SessionMapper.toSessionWeeksResponse(it) }

        return CustomResponse.ok(response)
    }
}
