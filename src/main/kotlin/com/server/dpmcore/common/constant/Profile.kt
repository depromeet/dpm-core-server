package com.server.dpmcore.common.constant

import org.springframework.core.env.Environment

enum class Profile(val value: String) {
    LOCAL("local"),
    DEV("dev"),
    PROD("prod"),
    ;

    companion object {
        fun get(environment: Environment): Profile {
            val active =
                environment.activeProfiles.firstOrNull()
                    ?: throw IllegalArgumentException("No active profile found")

            return entries.find { it.value.equals(active, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported profile: $active")
        }
    }
}
