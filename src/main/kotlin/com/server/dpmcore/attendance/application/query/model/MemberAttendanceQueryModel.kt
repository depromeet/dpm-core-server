package com.server.dpmcore.attendance.application.query.model

data class MemberAttendanceQueryModel(
    val id: Long,
    val name: String,
    val teamNumber: Int,
    val part: String,
    val lateCount: Int,
    val onlineAbsentCount: Int,
    val offlineAbsentCount: Int,
) {
    fun evaluateAttendanceStatus(
        impossibleThreshold: Int,
        atRiskThreshold: Int,
    ): String {
        val totalAbsence = onlineAbsentCount + offlineAbsentCount + (lateCount / 2)

        return when {
            totalAbsence >= impossibleThreshold -> AttendanceGraduationStatus.IMPOSSIBLE.name
            totalAbsence >= atRiskThreshold -> AttendanceGraduationStatus.AT_RISK.name
            else -> AttendanceGraduationStatus.NORMAL.name
        }
    }
}
