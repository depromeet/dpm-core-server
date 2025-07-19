package com.server.dpmcore.attendance.application.query.model

data class DetailMemberAttendanceQueryModel(
    val memberId: Long,
    val memberName: String,
    val teamNumber: Int,
    val part: String,
    val presentCount: Int,
    val lateCount: Int,
    val excusedAbsentCount: Int,
    val onlineAbsentCount: Int,
    val offlineAbsentCount: Int,
)
