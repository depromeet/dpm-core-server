package core.application.member.presentation.response

import core.domain.attendance.enums.AttendanceGraduationStatus
import core.domain.member.enums.MemberStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class MemberManagementResponse(
    val cohortId: Long,
    @field:Schema(description = "검색·필터와 무관한 현재 기수 현황")
    val summary: Summary,
    @field:Schema(description = "검색·필터 적용 후 목록 전체 수")
    val totalElements: Int,
    val page: Int,
    val size: Int,
    @field:Schema(description = "조회 대상 회원의 저장된 기본정보 변경/생성 시각 중 최댓값. 팀·역할만 바뀐 시각과 삭제는 포함하지 않음", nullable = true)
    val lastUpdatedAt: Instant?,
    val members: List<MemberSummary>,
) {
    @Schema(name = "MemberManagementTotals")
    data class Summary(
        @field:Schema(description = "제목 옆 현재 기수 총원. 디퍼+운영진+코어 합계, 무소속 대기자 제외")
        val totalMemberCount: Int,
        val deeperCount: Int,
        val organizerCount: Int,
        val coreCount: Int,
        @field:Schema(description = "현재 기수 또는 기수 없는 가입 대기자")
        val pendingCount: Int,
        @field:Schema(description = "승인 멤버 중 파트·팀·타입 하나라도 미배정인 인원")
        val missingInformationCount: Int,
        @field:Schema(description = "현재 기수 승인 멤버 중 AT_RISK와 IMPOSSIBLE의 합계")
        val graduationRiskCount: Int,
    )

    @Schema(name = "MemberManagementSummary")
    data class MemberSummary(
        val memberId: Long,
        val cohortId: Long?,
        val name: String,
        @field:Schema(description = "가입 이메일. 로그인별 이메일 또는 통합 후 대표 이메일을 의미하지 않음")
        val signupEmail: String,
        @field:Schema(description = "WEB/SERVER/DESIGN/IOS/ANDROID/UNASSIGNED")
        val part: String,
        @field:Schema(description = "DEEPER/ORGANIZER/CORE/UNASSIGNED. 내부 MASTER 권한은 표시하지 않음")
        val memberType: String,
        @field:Schema(description = "팀 번호. 미배정은 0")
        val teamNumber: Int,
        val status: MemberStatus,
        val missingInformation: Boolean,
        @field:Schema(description = "미승인 또는 출석 집계 자료가 없으면 null(미평가)", nullable = true)
        val graduationStatus: AttendanceGraduationStatus?,
        @field:Schema(description = "중복 의심 판별 기준 확정 전이므로 null(미평가). false로 해석하지 않음", nullable = true)
        val duplicateSuspected: Boolean?,
        @field:Schema(description = "저장된 기본정보 변경/생성 시각", nullable = true)
        val updatedAt: Instant?,
    )
}
