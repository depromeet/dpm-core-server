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
    @field:Schema(description = "회원 기본정보 변경/생성 시각 중 최댓값. 팀·역할·OAuth만 바뀐 시각과 삭제는 포함하지 않음", nullable = true)
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
        @field:Schema(description = "닉네임")
        val name: String,
        @field:Schema(
            description =
                "관리 표시 이메일. 카카오 우선, 미연결이면 애플·가입 이메일 순. " +
                    "카카오 연결의 이메일이 없으면 null. 같은 제공자는 가장 큰 연결 ID 기준",
            nullable = true,
        )
        val email: String?,
        @field:Schema(description = "WEB/SERVER/DESIGN/IOS/ANDROID/UNASSIGNED")
        val part: String,
        @field:Schema(description = "DEEPER/ORGANIZER/CORE/UNASSIGNED. 내부 MASTER 권한은 표시하지 않음")
        val memberType: String,
        @field:Schema(description = "팀 번호. 미배정은 0")
        val teamNumber: Int,
        val status: MemberStatus,
        val missingInformation: Boolean,
        @field:Schema(description = "현재 기수 승인 회원은 출석 기록이 없어도 0건으로 판정(NORMAL). 미승인·기수 미소속은 null", nullable = true)
        val graduationStatus: AttendanceGraduationStatus?,
        @field:Schema(description = "관리 대상 중 앞뒤 공백을 제외한 닉네임과 실제 파트가 같은 다른 회원 존재 여부. 대소문자 구분, 빈 닉네임·미배정 파트 제외")
        val duplicateSuspected: Boolean,
        @field:Schema(description = "저장된 기본정보 변경/생성 시각", nullable = true)
        val updatedAt: Instant?,
    )
}
