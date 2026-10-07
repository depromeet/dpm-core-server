package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.presentation.request.MemberBulkDeleteRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member", description = "멤버 API")
interface MemberDeletionApi {
    @Operation(
        summary = "멤버 영구 삭제 (명세)",
        description =
            "delete:member 권한이 필요합니다. 계정·로그인 수단·개인 정보를 정리하고 기존 토큰 접근을 차단합니다. " +
                "공지·과제·모임·정산 등 공유 기록과 다른 회원의 기록은 보존합니다. 삭제된 회원 재요청은 404입니다. " +
                "기존 v1 hard-delete의 연쇄 삭제 로직을 사용하지 않습니다. 현재는 데이터 변경 없이 MEMBER-501-01을 반환합니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "영구 삭제 완료 (구현 후 제공)", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "잘못된 멤버 ID"),
        ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
        ApiResponse(responseCode = "404", description = "멤버 없음"),
        ApiResponse(responseCode = "501", description = "MEMBER-501-01: 명세만 제공, 데이터 변경 없음"),
    )
    fun delete(memberId: Long): CustomResponse<Void>

    @Operation(
        summary = "멤버 일괄 영구 삭제 (명세)",
        description =
            "delete:member 권한이 필요합니다. 단건과 같은 삭제·공유 기록 보존 규칙을 적용합니다. " +
                "모든 대상을 한 트랜잭션으로 삭제하며 없는 회원이나 처리 실패가 하나라도 있으면 전체 취소합니다. " +
                "현재는 데이터 변경 없이 MEMBER-501-01을 반환합니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "일괄 영구 삭제 완료 (구현 후 제공)", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "빈 목록, 중복, null, 0 이하 또는 잘못된 ID"),
        ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
        ApiResponse(responseCode = "404", description = "대상 중 없는 멤버가 있음. 전체 취소"),
        ApiResponse(responseCode = "501", description = "MEMBER-501-01: 명세만 제공, 데이터 변경 없음"),
    )
    fun deleteBulk(request: MemberBulkDeleteRequest): CustomResponse<Void>
}
