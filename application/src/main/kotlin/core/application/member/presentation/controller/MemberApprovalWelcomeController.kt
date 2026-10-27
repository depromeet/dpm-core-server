package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.application.service.MemberApprovalWelcomeService
import core.application.security.annotation.CurrentMemberId
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v3/members/me/approval-welcome")
class MemberApprovalWelcomeController(private val service: MemberApprovalWelcomeService) : MemberApprovalWelcomeApi {
    @PreAuthorize("isAuthenticated()")
    @PatchMapping
    override fun confirm(
        @CurrentMemberId memberId: Long,
    ): CustomResponse<Void> {
        service.confirm(memberId)
        return CustomResponse.ok()
    }
}
