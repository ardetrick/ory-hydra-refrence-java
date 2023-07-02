plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal() // so that external plugins can be resolved in dependencies section
}

dependencies {
    implementation("io.freefair.gradle:lombok-plugin:8.1.0")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.0")
}

kotlin {
    jvmToolchain(17)
}
