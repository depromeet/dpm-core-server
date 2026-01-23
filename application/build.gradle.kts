plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.jib)
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.spring.boot.starter.jooq)
    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.kotlin.logging)
    implementation(libs.jsonwebtoken.jjwt)

//    TODO : Firebase 의존성 제거 후 application 모듈과 presentation 모듈에서는 몰라도 되게 리팩토링
    implementation(libs.firebase.admin)

    runtimeOnly(libs.mysql.connector)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.platform.launcher)

    implementation(project(":domain"))
    implementation(project(":persistence"))
}

jib {
    from {
        image = "eclipse-temurin:21-jre"
    }
    to {
        val dockerHubUser = System.getenv("DOCKER_USERNAME") ?: "dpm-core"
        val dockerHubRepository = System.getenv("DOCKER_REPOSITORY") ?: "dpm-core"

        image = "$dockerHubUser/$dockerHubRepository"

        auth {
            username = System.getenv("DOCKER_USERNAME")
            password = System.getenv("DOCKER_PASSWORD")
        }
    }
    container {
        ports = listOf("8080")
        jvmFlags = listOf("-Xms512m", "-Xmx512m", "-Duser.timezone=Asia/Seoul")
        environment = mapOf(
            "APPLE_PRIVATE_KEY_PATH" to "/app/secrets/AuthKey.p8"
        )
    }
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("dpm-core-server.jar")
}

springBoot {
    mainClass.set("core.application.CoreApplication")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}
