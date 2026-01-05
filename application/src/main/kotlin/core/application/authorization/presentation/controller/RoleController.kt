package core.application.authorization.presentation.controller

import core.application.authorization.presentation.response.RoleListResponse
import core.application.common.exception.CustomResponse
import core.domain.authorization.port.inbound.RoleQueryUseCase
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/roles")
class RoleController(
    private val roleQueryUseCase: RoleQueryUseCase,
) : RoleApi {
    @PreAuthorize("hasAuthority('read:authorization')")
    @GetMapping
    override fun getAllRoles(): CustomResponse<RoleListResponse> {
        val response = roleQueryUseCase.getAllRoles()

        return CustomResponse.ok(
            RoleListResponse.from(response),
        )
    }
}
