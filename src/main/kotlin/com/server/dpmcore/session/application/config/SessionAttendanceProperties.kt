package com.server.dpmcore.session.application.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "session.attendance")
class SessionAttendanceProperties {
    var startHour: Long = 14
}
