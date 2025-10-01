package core.application.authority.presentation.controller

import core.application.authority.application.service.AuthorityQueryService
import core.application.authority.presentation.response.AuthorityListResponse
import core.application.common.exception.CustomResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/authorities")
class AuthorityController(
    private val authorityQueryService: AuthorityQueryService,
) : AuthorityApi {
    @PreAuthorize("hasRole('ROLE_ORGANIZER')")
    @GetMapping
    override fun getAllAuthorities(): CustomResponse<AuthorityListResponse> {
        val response = authorityQueryService.getAllAuthorities()
        return CustomResponse.ok(response)
    }
}
