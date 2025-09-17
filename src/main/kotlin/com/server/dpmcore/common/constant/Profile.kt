package com.server.dpmcore.common.constant

import org.springframework.core.env.Environment

enum class Profile(val value: String) {
    LOCAL("local"),
    DEV("dev"),
    PROD("prod"),
    TEST("test"),
    ;

    companion object {
        fun get(environment: Environment): Profile {
            val active = environment.activeProfiles.firstOrNull()
            return entries.find { it.value.equals(active, ignoreCase = true) }
                ?: TEST // fallback
        }
    }
}
