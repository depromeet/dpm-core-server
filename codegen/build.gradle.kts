import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.Property

plugins {
    kotlin("jvm")
    id("nu.studer.jooq") version "9.0"
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val mysqlVersion = "8.0.33"

dependencies {
    implementation(project(":entity"))
    jooqGenerator(project(":entity"))
    implementation("org.jooq:jooq-meta:3.19.1")
    implementation("org.jooq:jooq-codegen:3.19.1")

    implementation("org.jooq:jooq:3.19.1")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    jooqGenerator("org.jooq:jooq-meta:3.19.1")
    jooqGenerator("org.jooq:jooq-codegen:3.19.1")
    jooqGenerator("org.jooq:jooq-meta-extensions-hibernate:3.19.1")
    jooqGenerator("com.mysql:mysql-connector-j:$mysqlVersion")
}

jooq {
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(true)

            jooqConfiguration.apply {
                logging = Logging.WARN
                jdbc = null

                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"

                    database.apply {
                        name = "org.jooq.meta.extensions.jpa.JPADatabase"

                        properties.addAll(
                            listOf(
                                Property().apply {
                                    key = "packages"
                                    value = "core.entity"
                                },
                                Property().apply {
                                    key = "useAttributeConverters"
                                    value = "true"
                                },
                                Property().apply {
                                    key = "defaultNameCase"
                                    value = "lower"
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
                                    key = "renderNameCase"
                                    value = "lower"
                                },
                            )
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
                                }
                            )
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
                        directory = "${project.layout.buildDirectory.get()}/generated"
                        packageName = "org.jooq.dsl"
                        encoding = "UTF-8"
                    }

                    strategy.apply {
                        name = "org.jooq.codegen.DefaultGeneratorStrategy"
                    }
                }
            }
        }
    }
}

tasks.register("prepareKotlinBuildScriptModel") {}

tasks.getByName<Jar>("jar") {
    enabled = true
}

tasks {
    withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
        enabled = false
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.named("generateJooq") {
    dependsOn(":entity:classes")
}

sourceSets {
    val main by getting {
        java {
            srcDir("${project.layout.buildDirectory.get()}/generated")
        }
    }
}
