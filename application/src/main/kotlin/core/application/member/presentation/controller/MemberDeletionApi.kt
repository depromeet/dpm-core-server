package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import core.application.member.presentation.request.MemberBulkDeleteRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member", description = "멤버 API")
interface MemberDeletionApi {
    @Operation(
        summary = "멤버 삭제",
        description =
            "delete:member 권한이 필요합니다. ACTIVE·INACTIVE 회원은 로그인 수단·세션과 개인 연결 데이터를 영구 삭제합니다. " +
                "PENDING·REJECTED 회원은 기존처럼 소프트 삭제합니다. 본인 계정은 삭제할 수 없습니다. " +
                "작성한 공지(과제 포함), 주최한 회식·정산·모임이 있으면 MEMBER-409-72로 차단합니다. " +
                "타인 기록과 병합 감사 이력은 유지합니다. 삭제된 회원 재요청은 404입니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "삭제 완료", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "잘못된 멤버 ID"),
        ApiResponse(responseCode = "403", description = "삭제 권한 없음 또는 본인 계정 삭제 요청"),
        ApiResponse(responseCode = "409", description = "MEMBER-409-72: 공지·회식·정산·모임 소유자가 포함됨. 전체 취소"),
        ApiResponse(responseCode = "404", description = "멤버 없음"),
    )
    fun delete(
        memberId: Long,
        @Parameter(hidden = true) actorMemberId: Long,
    ): CustomResponse<Void>

    @Operation(
        summary = "멤버 일괄 삭제",
        description =
            "단건과 같은 상태별 삭제 규칙을 적용합니다. 모든 대상을 한 트랜잭션으로 처리하며 " +
                "공유 기록 소유자, 본인, 없는 회원이나 이미 삭제된 회원이 포함되면 전체 취소합니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "일괄 삭제 완료", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "빈 목록, 중복, null, 0 이하 또는 잘못된 ID"),
        ApiResponse(responseCode = "403", description = "삭제 권한 없음 또는 본인 계정 삭제 요청"),
        ApiResponse(responseCode = "409", description = "MEMBER-409-72: 공지·회식·정산·모임 소유자가 포함됨. 전체 취소"),
        ApiResponse(responseCode = "404", description = "대상 중 없는 멤버가 있음. 전체 취소"),
    )
    fun deleteBulk(
        request: MemberBulkDeleteRequest,
        @Parameter(hidden = true) actorMemberId: Long,
    ): CustomResponse<Void>
}
