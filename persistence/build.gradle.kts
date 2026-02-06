plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.jooq)

    implementation(libs.firebase.admin)

    runtimeOnly(libs.mysql.connector)

    implementation(project(":domain"))
    implementation(project(":entity"))
    implementation(project(":codegen"))
}

tasks.getByName<Jar>("jar") {
    enabled = true
}

tasks.named("compileKotlin") {
    dependsOn(":codegen:generateJooq")
}
