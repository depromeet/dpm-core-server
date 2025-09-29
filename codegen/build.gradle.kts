import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.Property

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.jooq)
}

dependencies {
    implementation(libs.jooq)
    implementation(libs.jooq.meta)
    implementation(libs.jooq.codegen)
    implementation(libs.jooq.meta.extensions.hibernate)
    implementation(libs.spring.boot.starter.jooq)

    jooqGenerator(libs.jooq.meta)
    jooqGenerator(libs.jooq.codegen)
    jooqGenerator(libs.jooq.meta.extensions.hibernate)
    jooqGenerator(libs.mysql.connector)

    implementation(project(":entity"))
    jooqGenerator(project(":entity"))
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

tasks.getByName<Jar>("jar") {
    enabled = true
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    enabled = false
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
