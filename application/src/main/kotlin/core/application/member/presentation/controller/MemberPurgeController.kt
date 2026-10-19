package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.common.exception.GlobalExceptionCode
import core.application.member.application.service.MemberPurgeService
import core.application.security.annotation.CurrentMemberId
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestController
class MemberPurgeController(private val service: MemberPurgeService) : MemberPurgeApi {
    // MASTER 전용 권한이 없어 1차로 delete:member를 보고, MASTER 여부는 서비스에서 롤로 확인한다.
    @PreAuthorize("hasAuthority('delete:member')")
    @DeleteMapping("/v3/members/{memberId}/purge")
    override fun purge(
        @PathVariable memberId: Long,
        @CurrentMemberId actorMemberId: Long,
    ): CustomResponse<Void> {
        service.purge(memberId, actorMemberId)
        return CustomResponse.ok()
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleTypeMismatch(): CustomResponse<Void> = CustomResponse.error(GlobalExceptionCode.INVALID_INPUT)
}
