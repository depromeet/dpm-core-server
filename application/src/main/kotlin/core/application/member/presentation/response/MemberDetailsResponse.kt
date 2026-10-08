package core.application.member.presentation.response

import core.domain.attendance.enums.AttendanceGraduationStatus
import core.domain.attendance.port.outbound.query.AttendanceSummaryQueryModel
import core.domain.member.aggregate.Member
import core.domain.member.enums.LoginMethod
import core.domain.team.vo.TeamNumber
import io.swagger.v3.oas.annotations.media.Schema

data class MemberDetailsResponse(
    @field:Schema(
        description = "현재 세션의 로그인 수단에 해당하는 이메일. 로그인 수단을 알 수 없거나 저장된 이메일이 없으면 가입 이메일",
        example = "depromeetcore@gmail.com",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val email: String,
    @field:Schema(
        description = "이름",
        example = "디프만",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val name: String?,
    @field:Schema(
        description = "파트",
        example = "WEB",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    val part: String?,
    @field:Schema(
        description = "마지막 소속 기수",
        example = "17",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
    )
    val cohort: String?,
    @field:Schema(
        description = "마지막 소속 기수의 팀 번호. 팀이 없으면 설정된 기본 팀 번호(기본값 0)",
        example = "3",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    val teamNumber: TeamNumber,
    @field:Schema(
        description = "어드민 여부",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val isAdmin: Boolean,
    @field:Schema(
        description = "멤버 상태",
        example = "ACTIVE",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val status: String,
    @field:Schema(
        description = "현재 세션의 로그인 수단. 로그인 수단이 기록되기 전에 발급된 토큰이면 null",
        example = "KAKAO",
        allowableValues = ["KAKAO", "APPLE", "EMAIL"],
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true,
    )
    val loginMethod: String?,
    @field:Schema(
        description = "마지막 소속 기수가 현재 활성 기수인지 여부. 소속 기수가 없으면 false",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val isActiveCohort: Boolean,
    @field:Schema(
        description = "마지막 소속 기수의 출석 횟수. 소속 기수나 출석 기록이 없으면 0",
        example = "8",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val presentCount: Int,
    @field:Schema(
        description = "마지막 소속 기수의 지각 횟수. 소속 기수나 출석 기록이 없으면 0",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val lateCount: Int,
    @field:Schema(
        description = "마지막 소속 기수의 인정 결석 횟수. 소속 기수나 출석 기록이 없으면 0",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val excusedAbsentCount: Int,
    @field:Schema(
        description = "마지막 소속 기수의 결석 횟수(온라인+오프라인). 소속 기수나 출석 기록이 없으면 0",
        example = "0",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val absentCount: Int,
    @field:Schema(
        description =
            "마지막 소속 기수의 수료 상태. 현재 기수면 조회 시점의 수료 판정(NORMAL/AT_RISK/IMPOSSIBLE), " +
                "이전 기수면 COMPLETED, 소속 기수가 없으면 null",
        example = "NORMAL",
        allowableValues = ["NORMAL", "AT_RISK", "IMPOSSIBLE", "COMPLETED"],
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true,
    )
    val attendanceStatus: String?,
) {
    companion object {
        fun of(
            member: Member,
            email: String,
            isAdmin: Boolean,
            teamNumber: TeamNumber,
            loginMethod: LoginMethod?,
            isActiveCohort: Boolean,
            attendanceSummary: AttendanceSummaryQueryModel?,
            attendanceStatus: AttendanceGraduationStatus?,
        ): MemberDetailsResponse =
            MemberDetailsResponse(
                email = email,
                name = member.name,
                part = member.part?.name,
                cohort = member.latestCohortValue(),
                teamNumber = teamNumber,
                isAdmin = isAdmin,
                status = member.status.name,
                loginMethod = loginMethod?.name,
                isActiveCohort = isActiveCohort,
                presentCount = attendanceSummary?.presentCount ?: 0,
                lateCount = attendanceSummary?.lateCount ?: 0,
                excusedAbsentCount = attendanceSummary?.excusedAbsentCount ?: 0,
                absentCount = attendanceSummary?.absentCount ?: 0,
                attendanceStatus = attendanceStatus?.name,
            )
    }
}
