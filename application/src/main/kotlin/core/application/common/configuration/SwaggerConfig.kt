package core.application.common.configuration

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.config.Elements.JWT

@Configuration
class SwaggerConfig(
    private val environment: Environment,
) {
    companion object {
        private val PROFILE_SERVER_URL_MAP =
            mapOf(
                "local" to "http://localhost:8080",
                "dev" to "https://api.depromeet-core.shop",
                "prod" to "https://api.depromeet.com",
            )
    }

    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .info(swaggerInfo())
            .addSecurityItem(securityRequirement())
            .servers(initializeServers())
            .components(components())

    private fun swaggerInfo(): Info =
        Info()
            .version("v0.0.1")
            .title("디프만 코어")
            .description("디프만 코어 API 문서입니다.")

    private fun securityRequirement(): SecurityRequirement = SecurityRequirement().addList(JWT)

    private fun initializeServers(): List<Server> =
        PROFILE_SERVER_URL_MAP
            .filter { (key, _) -> environment.matchesProfiles(key) }
            .map { (key, value) -> openApiServer(value, "DEPROMEET CORE ${key.uppercase()}") }

    private fun openApiServer(
        url: String,
        description: String,
    ): Server =
        Server()
            .url(url)
            .description(description)

    private fun components(): Components =
        Components()
            .addSecuritySchemes(JWT, securityScheme())

    private fun securityScheme(): SecurityScheme =
        SecurityScheme()
            .name(JWT)
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat(JWT)
}
