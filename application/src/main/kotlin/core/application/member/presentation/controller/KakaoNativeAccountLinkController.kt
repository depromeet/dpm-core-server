package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.application.service.KakaoNativeAccountLinkService
import core.application.member.application.service.MemberAccountLinkRequestService
import core.application.member.presentation.response.MemberAccountResponse
import core.application.security.annotation.CurrentLoginIdentity
import core.application.security.annotation.CurrentMemberId
import core.domain.member.vo.LoginIdentity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v3/members/me/accounts/KAKAO/native")
@PreAuthorize("isAuthenticated()")
class KakaoNativeAccountLinkController(
    private val requests: MemberAccountLinkRequestService,
    private val links: KakaoNativeAccountLinkService,
) {
    @PostMapping("/authorization")
    @Operation(
        summary = "카카오 네이티브 계정 연결 시작",
        description =
            "현재 서비스 회원으로 인증해 호출합니다. 요청 body와 redirectUri는 필요하지 않습니다. " +
                "state 발급 후 네이티브 카카오 SDK로 인증하고, SDK의 accessToken과 state를 네이티브 연결 완료 API에 제출합니다. " +
                "state는 현재 회원과 네이티브 연결 흐름에 귀속되며 5분간 한 번만 사용합니다. " +
                "사용자가 취소하면 연결하지 않습니다. 웹/Apple 연결 API의 state와 서로 바꿔 쓸 수 없습니다.",
    )
    fun start(
        @CurrentMemberId memberId: Long,
        @CurrentLoginIdentity identity: LoginIdentity?,
    ): CustomResponse<NativeAccountLinkResponse> =
        CustomResponse.ok(NativeAccountLinkResponse(requests.startNativeKakao(memberId, identity)))

    @PostMapping
    @Operation(
        summary = "카카오 네이티브 계정 연결 완료",
        description =
            "연결 시작과 같은 서비스 회원으로 인증합니다. Authorization 헤더에는 서비스 액세스 토큰을 사용하고, " +
                "카카오 SDK accessToken은 body에 담습니다. 카카오 refreshToken은 보내지 않습니다. " +
                "일반 로그인 API를 호출하지 않으며 기존 memberId와 서비스 로그인 상태를 유지합니다. " +
                "다른 회원에 연결된 카카오 계정은 가져오거나 병합하지 않습니다. 이미 본인에게 연결됐다면 중복 생성하지 않습니다. " +
                "만료·소비·인증 실패 후에는 새 state로 다시 시작합니다. 배포 환경에 KAKAO_APP_ID 설정이 필요합니다.",
    )
    @ApiResponse(responseCode = "200", description = "연결 완료 후 연결 계정 목록")
    @ApiResponse(responseCode = "400", description = "MEMBER-400-51: state 만료·재사용·흐름 불일치 또는 카카오 인증 검증 실패")
    @ApiResponse(responseCode = "401", description = "인증 필요 또는 MEMBER-401-51: 현재 로그인 수단이 유효하지 않음")
    @ApiResponse(responseCode = "409", description = "MEMBER-409-21: 다른 회원에게 연결된 계정 또는 연결 소유권 충돌")
    fun complete(
        @CurrentMemberId memberId: Long,
        @CurrentLoginIdentity identity: LoginIdentity?,
        @Valid @RequestBody request: CompleteNativeAccountLinkRequest,
    ): CustomResponse<MemberAccountResponse> =
        CustomResponse.ok(
            links.complete(memberId, identity, request.state, request.accessToken),
        )
}

data class NativeAccountLinkResponse(
    @field:Schema(description = "현재 회원에 귀속된 일회용 43자 연결 요청값. 5분 유효") val state: String,
)

data class CompleteNativeAccountLinkRequest(
    @field:NotBlank @field:Size(min = 43, max = 43)
    @field:Schema(description = "네이티브 연결 시작 API에서 받은 state") val state: String,
    @field:NotBlank @field:Size(max = 4096)
    @field:Schema(description = "카카오 네이티브 SDK에서 받은 accessToken. 서비스 토큰이나 인가 코드가 아닙니다.") val accessToken: String,
)
