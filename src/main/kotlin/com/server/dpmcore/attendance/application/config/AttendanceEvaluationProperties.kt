package com.server.dpmcore.attendance.application.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "attendance.graduation")
data class AttendanceEvaluationProperties(
    var impossibleThreshold: Int = 4,
    var atRiskThreshold: Int = 3,
)
