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
    implementation(libs.spring.boot.starter.jooq)
    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.kotlin.logging)
    implementation(libs.jsonwebtoken.jjwt)

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
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
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

// 실제 MySQL 이 필요한 통합 테스트(@Tag("mysql-integration"))는 기본 test 에서 제외하고 별도 태스크로만 실행한다.
// 실행: DPM_IT_MYSQL_URL=jdbc:mysql://127.0.0.1:3307/dpm_it DPM_IT_MYSQL_USERNAME=root DPM_IT_MYSQL_PASSWORD=... \
//       ./gradlew :application:mysqlIntegrationTest
// 주의: 테스트가 스키마를 새로 만든다(ddl-auto=create). 로컬의 일회용 DB(dpm_it*)만 허용한다.
tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("mysql-integration")
    }
}

tasks.register<Test>("mysqlIntegrationTest") {
    description = "Runs MySQL integration tests (requires DPM_IT_MYSQL_URL)."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("mysql-integration")
    }
    shouldRunAfter(tasks.named("test"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}
