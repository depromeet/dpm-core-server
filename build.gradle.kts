import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.Property

plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("com.google.cloud.tools.jib") version "3.4.2"
    id("nu.studer.jooq") version "9.0"
}

group = "com.server"
version = "0.0.1-SNAPSHOT"

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    coloredOutput.set(true)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.JSON)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
    }
    filter {
        exclude("**/generated/**")
        include("**/*.kt, **/*.kts")
    }
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
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")

    implementation("io.jsonwebtoken:jjwt:0.12.6")

    // kotlin-jdsl
    val jdslVersion = "2.2.1.RELEASE"
    implementation("com.linecorp.kotlin-jdsl:spring-data-kotlin-jdsl-starter-jakarta:$jdslVersion")

    implementation("org.jooq:jooq:3.19.1")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    jooqGenerator("org.jooq:jooq-meta:3.19.1")
    jooqGenerator("org.jooq:jooq-codegen:3.19.1")
    jooqGenerator("org.jooq:jooq-meta-extensions:3.19.1")
    jooqGenerator("com.mysql:mysql-connector-j")

    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val dbHost: String = System.getProperty("DB_HOST") ?: "localhost"
val dbPort: String = System.getProperty("DB_PORT") ?: "3306"
val dbSchema: String = System.getProperty("DB_SCHEMA") ?: "dpm_core"
val dbUsername: String = System.getProperty("DB_USERNAME") ?: "root"
val dbPassword: String = System.getProperty("DB_PASSWORD") ?: "1234"

jooq {
    configurations {
        create("main") {
            // name of the jOOQ configuration
            generateSchemaSourceOnCompilation.set(true)

            jooqConfiguration.apply {
                logging = Logging.WARN
                jdbc = null

                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                        properties.addAll(
                            listOf(
                                Property().apply {
                                    key = "scripts"
                                    value = "src/main/resources/db/schema.sql"
                                },
                                Property().apply {
                                    key = "sort"
                                    value = "semantic"
                                },
                                Property().apply {
                                    key = "unqualifiedSchema"
                                    value = "none"
                                },
                                Property().apply {
                                    key = "defaultNameCase"
                                    value = "lower"
                                },
                            ),
                        )
                        forcedTypes.addAll(
                            listOf(
                                ForcedType().apply {
                                    name = "INSTANT"
                                    expression = ".*\\.date"
                                    types = "timestamp.*"
                                },
                                ForcedType().apply {
                                    name = "INSTANT"
                                    expression = ".*\\.attendance_start"
                                    types = "timestamp.*"
                                },
                                ForcedType().apply {
                                    name = "INSTANT"
                                    expression = ".*\\.attended_at"
                                    types = "timestamp.*"
                                },
                            ),
                        )
                    }
                    generate.apply {
                        isDaos = true
                        isRecords = true
                        isFluentSetters = true
                        isJavaTimeTypes = true
                        isDeprecated = false
                    }
                    target.apply {
                        directory = "build/generated-src/jooq"
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}

sourceSets {
    main {
        kotlin {
            srcDirs(listOf("src/main/kotlin", "src/generated", "build/generated-src/jooq"))
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("dpm-core-server.jar")
}

tasks.named("jib") {
    dependsOn("generateJooq")
}
