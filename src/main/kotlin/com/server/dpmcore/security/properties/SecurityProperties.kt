package com.server.dpmcore.security.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(prefix = "spring.security")
data class SecurityProperties(
    val logoutUrl: String,
    val coreRedirectUrl: String,
    val adminRedirectUrl: String,
    val restrictedRedirectUrl: String,
    @NestedConfigurationProperty val cookie: Cookie,
) {
    data class Cookie(
        val domain: String,
        val httpOnly: Boolean,
        val secure: Boolean,
    )
}
