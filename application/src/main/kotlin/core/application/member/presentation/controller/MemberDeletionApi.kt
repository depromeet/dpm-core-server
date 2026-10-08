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
        summary = "멤버 소프트 삭제",
        description =
            "delete:member 권한이 필요합니다. 회원만 소프트 삭제하고 모든 연관 데이터는 보존합니다. " +
                "로그인 수단·역할·기수·팀·출석·공지·과제·회식·모임·정산 기록도 삭제하거나 익명화하지 않습니다. " +
                "삭제된 회원의 로그인과 기존 토큰 접근은 차단하며, 삭제된 회원 재요청은 404입니다. " +
                "기존 hard-delete나 withdraw는 사용하지 않습니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "소프트 삭제 완료", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "잘못된 멤버 ID"),
        ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
        ApiResponse(responseCode = "404", description = "멤버 없음"),
    )
    fun delete(memberId: Long): CustomResponse<Void>

    @Operation(
        summary = "멤버 일괄 소프트 삭제",
        description =
            "delete:member 권한이 필요합니다. 단건과 같이 회원만 소프트 삭제하고 모든 연관 데이터는 보존합니다. " +
                "로그인과 기존 토큰 접근 차단도 단건과 같습니다. " +
                "모든 대상을 한 트랜잭션으로 처리하며 없는 회원이나 처리 실패가 하나라도 있으면 전체 취소합니다. " +
                "없는 회원 또는 이미 삭제된 회원이 있으면 404를 반환합니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "일괄 소프트 삭제 완료", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "빈 목록, 중복, null, 0 이하 또는 잘못된 ID"),
        ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
        ApiResponse(responseCode = "404", description = "대상 중 없는 멤버가 있음. 전체 취소"),
    )
    fun deleteBulk(request: MemberBulkDeleteRequest): CustomResponse<Void>
}
