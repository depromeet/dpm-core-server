jooq {
    version.set("3.19.1")
    configurations {
        create("testDB") {
            generationTool {
                generator {
                    database {
                        name = "org.jooq.meta.extensions.jpa.JPADatabase"
                        properties {
                            property {
                                key = "packages"
                                value = "core.persistence"
                            }
                            property {
                                key = "useAttributeConverters"
                                value = "true"
                            }
                        }
                    }
                    generate.apply {
                        isDeprecated = false
                        isDaos = true
                        isRecords = true
                        isImmutablePojos = true
                        isFluentSetters = true
                        isJavaTimeTypes = true
                    }
                    target {
                        packageName = "jooq.jooq_dsl"
                        directory = "$buildDir/generated/jooq"
                        encoding = "UTF-8"
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}

sourceSets {
    named("main") {
        java.srcDir("$buildDir/generated/jooq")
    }
}
