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
    fun evaluateAttendanceStatus(): String {
        val totalAbsence = onlineAbsentCount + offlineAbsentCount + (lateCount / 2)

        return when {
            totalAbsence >= 4 || offlineAbsentCount >= 2 -> AttendanceGraduationStatus.IMPOSSIBLE.name
            totalAbsence >= 3 -> AttendanceGraduationStatus.AT_RISK.name
            else -> AttendanceGraduationStatus.NORMAL.name
        }
    }
}
