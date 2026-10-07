package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.common.exception.GlobalExceptionCode
import core.application.member.application.exception.MemberManagementNotImplementedException
import core.application.member.presentation.request.MemberBadgeAcknowledgeRequest
import core.application.member.presentation.response.MemberBadgeCard
import core.application.member.presentation.response.MemberBadgeResponse
import core.application.member.presentation.response.MemberBadgesResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestController
@RequestMapping("/v3/members/badges")
class MemberBadgeController : MemberBadgeApi {
    @PreAuthorize("hasAuthority('read:member')")
    @GetMapping
    override fun getBadges(): CustomResponse<MemberBadgesResponse> = throw MemberManagementNotImplementedException()

    @PreAuthorize("hasAuthority('read:member')")
    @PostMapping("/{card}/acknowledgement")
    override fun acknowledge(
        @PathVariable card: MemberBadgeCard,
        @Valid @RequestBody request: MemberBadgeAcknowledgeRequest,
    ): CustomResponse<MemberBadgeResponse> = throw MemberManagementNotImplementedException()

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleTypeMismatch(): CustomResponse<Void> = CustomResponse.error(GlobalExceptionCode.INVALID_INPUT)
}
