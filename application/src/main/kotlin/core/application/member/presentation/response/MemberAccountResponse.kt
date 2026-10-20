package core.application.member.presentation.response

import core.domain.member.enums.LoginMethod
import io.swagger.v3.oas.annotations.media.Schema

data class MemberAccountResponse(
    @field:Schema(description = "연결된 카카오·애플 계정 전체와 실제 이메일/비밀번호 로그인 계정. 이메일은 조회만 지원")
    val accounts: List<ConnectedMemberAccount>,
)

data class ConnectedMemberAccount(
    @field:Schema(description = "로그인 방식별 연결 식별자. memberId가 아니며 소셜 해제 시 사용", example = "10")
    val accountId: Long,
    @field:Schema(description = "로그인 방식. EMAIL 항목에는 연결·해제 버튼을 제공하지 않음", example = "KAKAO")
    val loginMethod: LoginMethod,
    @field:Schema(description = "해당 연결의 이메일. 제공자가 이메일을 전달하지 않으면 null", example = "member@example.com", nullable = true)
    val email: String?,
    @field:Schema(description = "조회 시점의 해제 가능 여부. 마지막 로그인 수단 또는 EMAIL이면 false", example = "true")
    val unlinkAllowed: Boolean,
)
