plugins {
    kotlin("jvm")
    kotlin("plugin.jpa")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":entity"))
    implementation(project(":codegen"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-web")

    runtimeOnly("com.mysql:mysql-connector-j")
}

tasks.getByName<Jar>("jar") {
    enabled = true
}

tasks.named("compileKotlin") {
    dependsOn(":codegen:generateJooq")
}
