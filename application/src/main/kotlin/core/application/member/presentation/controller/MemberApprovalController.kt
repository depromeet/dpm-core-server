package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.application.service.MemberApprovalService
import core.application.member.presentation.request.MemberApprovalRequest
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v3/members")
class MemberApprovalController(
    private val service: MemberApprovalService,
) : MemberApprovalApi {
    @PreAuthorize("hasAuthority('create:member')")
    @PatchMapping("/whitelist")
    override fun approve(
        @Valid @RequestBody request: MemberApprovalRequest,
    ): CustomResponse<Void> {
        service.approve(request.members)
        return CustomResponse.ok()
    }
}
