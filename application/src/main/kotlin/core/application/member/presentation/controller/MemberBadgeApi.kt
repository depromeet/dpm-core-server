package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.presentation.request.MemberBadgeAcknowledgeRequest
import core.application.member.presentation.response.MemberBadgeCard
import core.application.member.presentation.response.MemberBadgeResponse
import core.application.member.presentation.response.MemberBadgesResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member", description = "멤버 API")
interface MemberBadgeApi {
    @Operation(
        summary = "멤버 관리 NEW 조회 (명세)",
        description =
            "read:member 권한으로 현재 기수의 미승인(PENDING), 정보 미입력(INCOMPLETE), 수료 위험(AT_RISK) 카드 상태를 조회합니다. " +
                "카드 대상에 신규 진입하거나 재진입한 회원이 있으면 NEW를 표시합니다. 대상 이탈만으로 NEW를 생성하지 않습니다. " +
                "동일 인원 수에서 대상이 바뀌어도 새 진입을 반영합니다. 위험 카드에는 수료 위험·불가를 모두 포함합니다. " +
                "운영진 전체가 확인 상태를 공유하며 검색·필터나 조회만으로 해제하지 않습니다. " +
                "현재는 MEMBER-501-01을 반환합니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "현재 기수 카드별 NEW 상태 (구현 후 제공)", useReturnTypeSchema = true),
        ApiResponse(
            responseCode = "403",
            description = "멤버 조회 권한 없음",
            content = [Content(schema = Schema(implementation = CustomResponse::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "현재 기수 없음",
            content = [Content(schema = Schema(implementation = CustomResponse::class))],
        ),
        ApiResponse(
            responseCode = "501",
            description = "MEMBER-501-01: 명세만 제공. data 필드는 반환하지 않음",
            content = [Content(schema = Schema(implementation = CustomResponse::class))],
        ),
    )
    fun getBadges(): CustomResponse<MemberBadgesResponse>

    @Operation(
        summary = "멤버 관리 NEW 확인 (명세)",
        description =
            "read:member 권한이 필요합니다. 카드 클릭 시 조회 응답의 cohortId와 해당 카드 version을 전달합니다. " +
                "한 운영진이 확인하면 해당 버전까지 모든 운영진에게 해제됩니다. 오래된 버전이나 반복 확인은 " +
                "이미 확인한 상태를 되돌리거나 더 최근의 NEW를 지우지 않습니다. 확인 후 최신 카드 상태를 반환합니다. " +
                "존재할 수 없는 미래 버전은 400, 현재 기수와 다른 cohortId는 409로 거절하며 다시 조회해야 합니다. " +
                "운영진별 상태나 실시간 푸시는 제공하지 않습니다. 현재는 데이터 변경 없이 MEMBER-501-01을 반환합니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "확인 후 최신 카드 상태 (구현 후 제공)", useReturnTypeSchema = true),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 카드, 기수 ID 또는 버전. 미래 버전 포함",
            content = [Content(schema = Schema(implementation = CustomResponse::class))],
        ),
        ApiResponse(
            responseCode = "403",
            description = "멤버 조회 권한 없음",
            content = [Content(schema = Schema(implementation = CustomResponse::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "관리 기수가 변경됨. NEW 조회 필요",
            content = [Content(schema = Schema(implementation = CustomResponse::class))],
        ),
        ApiResponse(
            responseCode = "501",
            description = "MEMBER-501-01: 명세만 제공, 데이터 변경 없음. data 필드는 반환하지 않음",
            content = [Content(schema = Schema(implementation = CustomResponse::class))],
        ),
    )
    fun acknowledge(
        card: MemberBadgeCard,
        request: MemberBadgeAcknowledgeRequest,
    ): CustomResponse<MemberBadgeResponse>
}
