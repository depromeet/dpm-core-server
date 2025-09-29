package core.application.common.configuration

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EntityScan(basePackages = ["core.entity"])
@EnableJpaRepositories(basePackages = ["core.persistence"])
class JpaConfig
