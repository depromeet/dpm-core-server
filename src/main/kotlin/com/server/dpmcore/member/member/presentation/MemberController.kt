package com.server.dpmcore.member.member.presentation

import com.server.dpmcore.common.exception.CustomResponse
import com.server.dpmcore.member.member.application.MemberService
import com.server.dpmcore.member.member.domain.model.MemberId
import com.server.dpmcore.member.member.presentation.request.ChangeMemberStatusRequest
import com.server.dpmcore.member.member.presentation.response.MemberDetailsResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/members")
class MemberController(
    private val memberService: MemberService,
) : MemberApi {
    @GetMapping("/me")
    override fun me(memberId: MemberId): CustomResponse<MemberDetailsResponse> {
        val response: MemberDetailsResponse = memberService.memberMe(memberId)
        return CustomResponse.ok(response)
    }

    @PatchMapping("/withdraw")
    override fun withdraw(
        memberId: MemberId,
        response: HttpServletResponse,
    ): CustomResponse<Void> {
        memberService.withdraw(memberId, response)
        return CustomResponse.noContent()
    }

    @PatchMapping("/status")
    override fun changeMemberStatus(
        memberId: MemberId,
        request: ChangeMemberStatusRequest,
    ): CustomResponse<Void> {
        memberService.changeMemberStatus(memberId, request.status)
        return CustomResponse.noContent()
    }
}
