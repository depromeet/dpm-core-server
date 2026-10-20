package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.application.service.MemberAccountLinkRequestService
import core.application.member.application.service.MemberAccountLinkService
import core.application.member.application.service.MemberAccountService
import core.application.member.presentation.response.MemberAccountResponse
import core.application.security.annotation.CurrentLoginIdentity
import core.application.security.annotation.CurrentMemberId
import core.application.security.oauth.service.OAuthAuthorizationUrl
import core.domain.member.enums.OAuthProvider
import core.domain.member.vo.LoginIdentity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v3/members/me/accounts")
@PreAuthorize("isAuthenticated()")
class MemberAccountController(
    private val accounts: MemberAccountService,
    private val requests: MemberAccountLinkRequestService,
    private val links: MemberAccountLinkService,
) {
    @GetMapping
    @Operation(
        summary = "내 연결 계정 조회",
        description = "연결된 소셜 계정을 모두 반환하며 실제 이메일 로그인 계정은 조회만 지원합니다. accountId는 loginMethod별 식별자입니다.",
    )
    fun get(
        @CurrentMemberId memberId: Long,
    ): CustomResponse<MemberAccountResponse> = CustomResponse.ok(accounts.get(memberId))

    @PostMapping("/{provider}/authorization")
    @Operation(
        summary = "소셜 계정 연결 시작",
        description = "KAKAO/APPLE 인증 URL을 발급합니다. 로그인한 회원에게 귀속된 state는 5분간 한 번 사용할 수 있습니다. 인증 완료 후 콜백으로 받은 code와 state를 연결 완료 API에 제출합니다.",
    )
    fun start(
        @CurrentMemberId memberId: Long,
        @CurrentLoginIdentity identity: LoginIdentity?,
        @PathVariable provider: OAuthProvider,
        @Valid @RequestBody request: StartAccountLinkRequest,
    ): CustomResponse<OAuthAuthorizationUrl> =
        CustomResponse.ok(requests.start(memberId, identity, provider, request.redirectUri))

    @PostMapping("/{provider}")
    @Operation(
        summary = "소셜 계정 연결 완료",
        description = "연결 시작 API의 state와 제공자 인증 코드를 제출합니다. 이미 다른 회원에게 연결된 계정은 409로 거절하며 회원을 통합하지 않습니다. 같은 계정을 다시 연결해도 중복 저장하지 않습니다. 실패/만료 시 인증부터 다시 시작해주세요.",
    )
    fun link(
        @CurrentMemberId memberId: Long,
        @CurrentLoginIdentity identity: LoginIdentity?,
        @PathVariable provider: OAuthProvider,
        @Valid @RequestBody request: CompleteAccountLinkRequest,
    ): CustomResponse<MemberAccountResponse> =
        CustomResponse.ok(links.complete(memberId, identity, provider, request.state, request.authorizationCode))

    @DeleteMapping("/social/{accountId}")
    @Operation(
        summary = "소셜 계정 연결 해제",
        description = "조회 응답의 소셜 accountId로 해제합니다. 마지막 로그인 수단은 409이며 이메일 해제는 지원하지 않습니다. 해당 수단의 기존 로그인 토큰은 즉시 사용할 수 없고, 해제한 계정으로 로그인하면 신규 가입 절차로 이동합니다. 제공자 서비스의 동의 철회와는 별개입니다.",
    )
    fun unlink(
        @CurrentMemberId memberId: Long,
        @CurrentLoginIdentity identity: LoginIdentity?,
        @PathVariable accountId: Long,
    ): CustomResponse<MemberAccountResponse> = CustomResponse.ok(accounts.unlink(memberId, identity, accountId))
}

data class StartAccountLinkRequest(
    @field:NotBlank @field:Size(max = 2048) val redirectUri: String,
)

data class CompleteAccountLinkRequest(
    @field:NotBlank @field:Size(max = 4096) val authorizationCode: String,
    @field:NotBlank @field:Size(
        min = 43,
        max = 43,
    ) @field:Schema(description = "연결 시작 시 발급되고 제공자 콜백에서 반환된 state") val state: String,
)
