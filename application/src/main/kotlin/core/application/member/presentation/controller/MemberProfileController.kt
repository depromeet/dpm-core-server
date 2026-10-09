package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.application.service.MemberProfileService
import core.application.member.presentation.request.MemberProfileUpdateRequest
import core.application.member.presentation.response.MemberProfileResponse
import core.application.security.annotation.CurrentMemberId
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v3/members/me/profile")
class MemberProfileController(private val profiles: MemberProfileService) : MemberProfileApi {
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    override fun get(@CurrentMemberId memberId: Long): CustomResponse<MemberProfileResponse> =
        CustomResponse.ok(profiles.get(memberId))

    @PreAuthorize("isAuthenticated()")
    @PatchMapping
    override fun complete(
        @CurrentMemberId memberId: Long,
        @Valid @RequestBody request: MemberProfileUpdateRequest,
    ): CustomResponse<MemberProfileResponse> = CustomResponse.ok(profiles.complete(memberId, request.name, request.part))
}
