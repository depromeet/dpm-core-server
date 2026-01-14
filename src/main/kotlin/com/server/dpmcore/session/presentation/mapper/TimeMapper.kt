package com.server.dpmcore.session.presentation.mapper

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeMapper {
    fun instantToLocalDateTime(instant: Instant): LocalDateTime =
        LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Seoul"))

    fun instantToLocalDateTimeString(instant: Instant): String =
        instantToLocalDateTime(instant).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    fun localDateTimeToInstant(localDateTime: LocalDateTime): Instant =
        localDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()
}
