package com.server.dpmcore.attendance.application

import com.server.dpmcore.attendance.application.config.AttendanceEvaluationProperties
import com.server.dpmcore.attendance.application.query.model.AttendanceGraduationStatus
import org.springframework.stereotype.Component

@Component
class AttendanceGraduationEvaluator(
    private val attendanceEvaluationProperties: AttendanceEvaluationProperties,
) {
    fun evaluate(
        onlineAbsentCount: Int,
        offlineAbsentCount: Int,
        lateCount: Int,
    ): String {
        val totalAbsence = onlineAbsentCount + offlineAbsentCount + (lateCount / 2)

        return when {
            totalAbsence >= attendanceEvaluationProperties.impossibleThreshold ->
                AttendanceGraduationStatus.IMPOSSIBLE.name
            totalAbsence >= attendanceEvaluationProperties.atRiskThreshold ->
                AttendanceGraduationStatus.AT_RISK.name
            else -> AttendanceGraduationStatus.NORMAL.name
        }
    }
}
