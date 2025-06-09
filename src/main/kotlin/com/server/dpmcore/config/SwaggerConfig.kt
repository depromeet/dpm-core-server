package com.server.dpmcore.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun OpenApi(): OpenAPI {
        return OpenAPI()
            .info(swaggerInfo())
    }

    private fun swaggerInfo(): Info {
        return Info()
            .version("v0.0.1")
            .title("디프만 코어")
            .description("디프만 코어 API 문서입니다.")
    }

}