package core.application.authorization.presentation.controller

import core.application.authorization.application.service.RoleCommandService
import core.application.authorization.presentation.request.UpdateMemberRoleRequest
import core.application.authorization.presentation.response.MemberRoleListResponse
import core.application.authorization.presentation.response.MemberRoleResponse
import core.application.authorization.presentation.response.RoleListResponse
import core.application.common.exception.CustomResponse
import core.domain.authorization.port.inbound.RoleQueryUseCase
import core.domain.member.vo.MemberId
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/roles")
class RoleController(
    private val roleQueryUseCase: RoleQueryUseCase,
    private val roleCommandService: RoleCommandService,
) : RoleApi {
    @PreAuthorize("hasAuthority('read:authorization')")
    @GetMapping
    override fun getAllRoles(
        @RequestParam(required = false) cohort: String?,
    ): CustomResponse<RoleListResponse> {
        val response =
            cohort
                ?.let { roleQueryUseCase.getRolesByCohort(it) }
                ?: roleQueryUseCase.getAllRoles()

        return CustomResponse.ok(
            RoleListResponse.from(response),
        )
    }

    @PreAuthorize("hasAuthority('read:authorization')")
    @GetMapping("/members/{memberId}")
    override fun getMemberRoles(
        @PathVariable memberId: MemberId,
    ): CustomResponse<MemberRoleResponse> =
        CustomResponse.ok(
            MemberRoleResponse.of(
                memberId = memberId,
                roles = roleQueryUseCase.getRoleNamesByMemberId(memberId),
            ),
        )

    @PreAuthorize("hasAuthority('read:authorization')")
    @GetMapping("/members")
    override fun getMembersRoles(
        @RequestParam memberIds: List<Long>,
    ): CustomResponse<MemberRoleListResponse> =
        CustomResponse.ok(
            MemberRoleListResponse.from(
                roleQueryUseCase.getRoleNamesByMemberIds(
                    memberIds.map(::MemberId),
                ),
            ),
        )

    @PreAuthorize("hasAuthority('update:authorization')")
    @RequestMapping("/members/{memberId}", method = [RequestMethod.PATCH, RequestMethod.PUT])
    override fun updateMemberRole(
        @PathVariable memberId: MemberId,
        @Valid @RequestBody request: UpdateMemberRoleRequest,
    ): CustomResponse<Void> {
        roleCommandService.updateMemberRole(memberId, request)
        return CustomResponse.ok()
    }
}
