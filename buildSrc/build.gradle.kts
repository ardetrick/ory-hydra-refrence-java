plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal() // so that external plugins can be resolved in dependencies section
}

dependencies {
    implementation(libs.freefair.lombok.plugin)
    implementation(libs.spring.dependency.management.plugin)
    implementation(libs.foojay.resolver)
}

kotlin {
    jvmToolchain(21)
}
