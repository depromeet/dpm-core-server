plugins {
    kotlin("jvm")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":domain"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
