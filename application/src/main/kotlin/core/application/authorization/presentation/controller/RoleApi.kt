package core.application.authorization.presentation.controller

import core.application.authorization.presentation.response.MemberRoleListResponse
import core.application.authorization.presentation.response.MemberRoleResponse
import core.application.authorization.presentation.response.RoleListResponse
import core.application.common.exception.CustomResponse
import core.application.member.presentation.request.UpdateMemberRoleRequest
import core.domain.member.vo.MemberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Role", description = "역할 API")
interface RoleApi {
    @ApiResponse(responseCode = "200", description = "역할 목록 조회 성공")
    @Operation(
        summary = "역할 목록 조회 API",
        description =
            "역할 목록을 조회 합니다. cohort를 전달하지 않으면 최신 기수 역할 목록을 조회합니다. " +
                "정산서 생성 시 역할 별로 회식 인원을 초대하기 위해 사용 됩니다.",
    )
    fun getAllRoles(
        @Parameter(description = "조회할 기수 값. 예: 17 또는 17기", example = "17")
        cohort: String?,
    ): CustomResponse<RoleListResponse>

    @ApiResponse(responseCode = "200", description = "멤버 역할 조회 성공")
    @Operation(
        summary = "멤버 역할 조회 API",
        description = "memberId에 부여된 역할 목록을 조회합니다.",
    )
    fun getMemberRoles(
        @Parameter(description = "조회 대상 멤버 식별자", example = "1")
        memberId: MemberId,
    ): CustomResponse<MemberRoleResponse>

    @ApiResponse(responseCode = "200", description = "멤버 목록 역할 조회 성공")
    @Operation(
        summary = "멤버 목록 역할 조회 API",
        description = "memberIds에 포함된 멤버들의 역할 목록을 조회합니다.",
    )
    fun getMembersRoles(
        @Parameter(description = "조회 대상 멤버 식별자 목록", example = "1,2,3")
        memberIds: List<Long>,
    ): CustomResponse<MemberRoleListResponse>

    @ApiResponse(responseCode = "200", description = "멤버 역할 변경 성공")
    @Operation(
        summary = "멤버 기수별 역할 변경 API",
        description = "isAdmin=true면 해당 기수 운영진, false면 해당 기수 디퍼로 변경합니다.",
    )
    fun updateMemberRole(
        @Parameter(description = "역할 변경 대상 멤버 식별자", example = "1")
        memberId: MemberId,
        request: UpdateMemberRoleRequest,
    ): CustomResponse<Void>
}
