plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal() // so that external plugins can be resolved in dependencies section
}

dependencies {
    implementation("io.freefair.gradle:lombok-plugin:8.4")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.4")
    implementation("org.gradle.toolchains:foojay-resolver:0.8.0")
}

kotlin {
    jvmToolchain(17)
}
