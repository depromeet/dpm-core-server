package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.application.service.MemberManagementQueryService
import core.application.member.presentation.request.MemberManagementRequest
import core.application.member.presentation.response.MemberManagementResponse
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v3/members")
class MemberManagementController(
    private val queryService: MemberManagementQueryService,
) : MemberManagementApi {
    @PreAuthorize("hasAuthority('read:member')")
    @GetMapping("/overview")
    override fun getOverview(
        @Valid @ModelAttribute request: MemberManagementRequest,
    ): CustomResponse<MemberManagementResponse> = CustomResponse.ok(queryService.getOverview(request))
}
