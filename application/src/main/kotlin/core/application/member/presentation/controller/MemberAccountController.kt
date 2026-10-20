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
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
        description =
            "로그인이 필요하며 관리자 권한은 필요하지 않습니다. 연결된 카카오·애플 계정과 실제 이메일/비밀번호 로그인 자격증명을 조회합니다. " +
                "소셜 계정에 등록된 이메일만으로 EMAIL 항목을 만들지 않습니다. EMAIL은 조회만 지원하며 연결·해제할 수 없습니다. " +
                "accountId는 memberId가 아닌 loginMethod별 연결 식별자이며 제공자가 이메일을 주지 않으면 email은 null입니다.",
    )
    fun get(
        @CurrentMemberId memberId: Long,
    ): CustomResponse<MemberAccountResponse> = CustomResponse.ok(accounts.get(memberId))

    @PostMapping("/{provider}/authorization")
    @Operation(
        summary = "소셜 계정 연결 시작",
        description =
            "로그인이 필요하며 관리자 권한은 필요하지 않습니다. KAKAO/APPLE만 지원합니다. redirectUri는 서버에 허용된 origin의 " +
                "절대 URL이어야 합니다. 응답의 authorizationUrl을 수정하지 않고 그대로 열어 인증합니다. state는 현재 회원과 제공자에 " +
                "귀속되며 5분간 한 번 사용할 수 있습니다. Apple은 response_mode=form_post이므로 콜백에서 POST form의 code와 " +
                "state를 받아야 합니다. 콜백의 code를 authorizationCode에, state를 state에 담아 연결 완료 API로 제출합니다. " +
                "연결 과정에서 일반 로그인 API를 호출하지 않습니다.",
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
        description =
            "연결을 시작한 회원으로 로그인한 상태에서 호출하며 관리자 권한은 필요하지 않습니다. 제공자 콜백의 code와 state를 " +
                "authorizationCode와 state로 제출합니다. 이미 본인에게 연결된 계정은 중복 생성하지 않지만 소비한 state는 재사용할 수 " +
                "없습니다. 다른 회원의 계정을 가져오거나 회원을 통합하지 않습니다. 만료·소비·실패 후에는 연결 시작부터 다시 진행합니다.",
    )
    @ApiResponse(responseCode = "200", description = "연결 완료 후 연결 계정 목록 반환")
    @ApiResponse(responseCode = "400", description = "MEMBER-400-51: 잘못된 요청, 만료 또는 이미 소비한 state, 제공자 인증 실패")
    @ApiResponse(responseCode = "401", description = "인증 필요 또는 MEMBER-401-51: 로그인 수단이 해제되어 재로그인 필요")
    @ApiResponse(responseCode = "409", description = "MEMBER-409-21: 다른 회원에게 연결된 계정 또는 연결 소유권 충돌")
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
        description =
            "로그인이 필요하며 관리자 권한은 필요하지 않습니다. 조회 응답 중 KAKAO/APPLE 항목의 accountId로 해제합니다. " +
                "memberId나 EMAIL 항목의 accountId를 보내지 마세요. 마지막 로그인 수단과 이메일 해제는 지원하지 않습니다. " +
                "해제한 수단의 기존 로그인 토큰은 즉시 사용할 수 없습니다. 현재 로그인한 수단을 해제했다면 남아 있는 다른 수단으로 " +
                "다시 로그인해야 합니다. 해제한 계정으로 일반 로그인하면 신규 가입 절차로 이동하며 제공자 서비스의 동의 철회와는 별개입니다.",
    )
    @ApiResponse(responseCode = "200", description = "해제 후 남아 있는 연결 계정 목록 반환")
    @ApiResponse(responseCode = "409", description = "MEMBER-409-51: 마지막 로그인 수단이므로 해제 불가")
    fun unlink(
        @CurrentMemberId memberId: Long,
        @CurrentLoginIdentity identity: LoginIdentity?,
        @PathVariable accountId: Long,
    ): CustomResponse<MemberAccountResponse> = CustomResponse.ok(accounts.unlink(memberId, identity, accountId))
}

data class StartAccountLinkRequest(
    @field:Schema(
        description = "서버에서 허용한 origin의 제공자 인증 콜백 절대 URL",
        example = "https://example.com/login/apple/callback",
    )
    @field:NotBlank @field:Size(max = 2048) val redirectUri: String,
)

data class CompleteAccountLinkRequest(
    @field:Schema(description = "제공자 콜백의 code 값. 액세스 토큰이나 ID 토큰이 아닙니다.", example = "provider-authorization-code")
    @field:NotBlank @field:Size(max = 4096) val authorizationCode: String,
    @field:NotBlank @field:Size(
        min = 43,
        max = 43,
    ) @field:Schema(
        description = "연결 시작 시 발급되고 제공자 콜백에서 반환된 43자 state. 5분 유효, 한 번만 사용 가능",
        example = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG",
    ) val state: String,
)
