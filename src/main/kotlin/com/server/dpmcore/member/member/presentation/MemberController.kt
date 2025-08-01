package com.server.dpmcore.member.member.presentation

import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.member.member.application.MemberCommandService
import com.server.dpmcore.member.member.application.MemberQueryService
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.presentation.request.InitMemberDataRequest
import com.server.dpmcore.member.member.presentation.response.MemberDetailsResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/members")
class MemberController(
    private val memberQueryService: MemberQueryService,
    private val memberCommandService: MemberCommandService,
) : MemberApi {
    @PreAuthorize("!hasRole('ROLE_GUEST')")
    @GetMapping("/me")
    override fun me(memberId: MemberId): CustomResponse<MemberDetailsResponse> {
        val response: MemberDetailsResponse = memberQueryService.memberMe(memberId)
        return CustomResponse.ok(response)
    }

    @PatchMapping("/withdraw")
    override fun withdraw(
        memberId: MemberId,
        response: HttpServletResponse,
    ): CustomResponse<Void> {
        memberQueryService.withdraw(memberId, response)
        return CustomResponse.noContent()
    }

    @PreAuthorize("hasRole('ROLE_ORGANIZER')")
    @PatchMapping("/init")
    override fun initMemberDataAndApprove(
        @RequestBody request: InitMemberDataRequest,
    ): CustomResponse<Void> {
        memberCommandService.initMemberDataAndApprove(request)
        return CustomResponse.noContent()
    }
}
