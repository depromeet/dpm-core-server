plugins {
    kotlin("jvm")
}

dependencies {

}

tasks.register("prepareKotlinBuildScriptModel") {}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
