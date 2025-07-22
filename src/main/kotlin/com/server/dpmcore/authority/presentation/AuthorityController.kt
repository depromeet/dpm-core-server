package com.server.dpmcore.authority.presentation

import com.server.dpmcore.authority.application.AuthorityQueryService
import com.server.dpmcore.authority.presentation.response.AuthorityListResponse
import com.server.dpmcore.common.exception.CustomResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/authorities")
class AuthorityController(
    private val authorityQueryService: AuthorityQueryService,
) : AuthorityApi {
    @GetMapping
    override fun getAllAuthorities(): CustomResponse<AuthorityListResponse> {
        val response = authorityQueryService.getAllAuthorities()
        return CustomResponse.ok(response)
    }
}
