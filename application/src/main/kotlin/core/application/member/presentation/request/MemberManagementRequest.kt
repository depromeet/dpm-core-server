package core.application.member.presentation.request

import core.domain.attendance.enums.AttendanceGraduationStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class MemberManagementRequest(
    @field:Schema(description = "승인 탭. APPROVED는 ACTIVE/INACTIVE를 포함")
    val approvalStatus: ApprovalStatus = ApprovalStatus.APPROVED,
    @field:Schema(description = "닉네임 또는 관리 목록에 표시하는 이메일 부분 검색. 앞뒤 공백 무시, 대소문자 구분 없음")
    @field:Size(max = 255)
    val search: String? = null,
    @field:Schema(description = "파트. 미배정은 UNASSIGNED")
    @field:Pattern(regexp = "WEB|SERVER|DESIGN|IOS|ANDROID|UNASSIGNED")
    val part: String? = null,
    @field:Schema(description = "팀 번호. 미배정은 0")
    @field:Min(0)
    val teamNumber: Int? = null,
    @field:Schema(description = "활동 상태. INACTIVE는 활동 정지/기수 종료를 함께 표현")
    @field:Pattern(regexp = "ACTIVE|INACTIVE")
    val status: String? = null,
    @field:Schema(description = "운영진·코어 제외. 기본 true")
    val excludeStaff: Boolean = true,
    @field:Schema(description = "true이면 승인 멤버 중 파트·팀·타입이 하나라도 미배정인 멤버만 조회")
    val missingInformationOnly: Boolean = false,
    @field:Schema(description = "수료 상태 필터. 위험 카드는 AT_RISK, IMPOSSIBLE을 함께 전달. 미평가 회원은 제외")
    val graduationStatuses: List<AttendanceGraduationStatus>? = null,
    @field:Min(1)
    val page: Int = 1,
    @field:Min(1)
    @field:Max(100)
    val size: Int = 20,
) {
    enum class ApprovalStatus { PENDING, APPROVED }
}
