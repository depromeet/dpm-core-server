package core.application.member.presentation.controller

import core.application.common.exception.CustomResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Member", description = "멤버 API")
interface MemberPurgeApi {
    @Operation(
        summary = "멤버 영구 삭제 (MASTER 전용)",
        description =
            "MASTER 롤만 호출할 수 있습니다. 회원 행과 그 회원을 참조하는 모든 데이터를 되돌릴 수 없게 삭제합니다. " +
                "로그인 수단·토큰·역할·권한·기수·팀·출석·불참 사유·세션 피드백·이미지(버킷 객체 포함)·공지 읽음·과제 제출, " +
                "본인이 작성한 공지·과제, 본인이 만든 회식·정산·모임과 그 하위 데이터까지 함께 삭제합니다. " +
                "소프트 삭제·탈퇴한 회원도 대상이며, 본인 계정은 삭제할 수 없습니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "영구 삭제 완료", useReturnTypeSchema = true),
        ApiResponse(responseCode = "400", description = "잘못된 멤버 ID"),
        ApiResponse(responseCode = "403", description = "MASTER가 아니거나 본인 계정 삭제 요청"),
        ApiResponse(responseCode = "404", description = "멤버 없음"),
    )
    fun purge(
        memberId: Long,
        @Parameter(hidden = true) actorMemberId: Long,
    ): CustomResponse<Void>
}
