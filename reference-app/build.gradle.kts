plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spotless)
    id("project.java-conventions")
}

spotless {
    java {
        googleJavaFormat(libs.versions.googleJavaFormat.get())
        formatAnnotations()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        ktlint()
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-freemarker")
    implementation("org.springframework.boot:spring-boot-starter-jersey")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.session:spring-session-core")
    implementation(libs.ory.hydra.client)

    testImplementation(libs.testcontainers.ory.hydra)
    testImplementation(libs.auth0.java.jwt)
    testImplementation(libs.microsoft.playwright)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint")
    // -Werror fails the build on any lint warning — notably uses of deprecated/for-removal
    // upstream API. Intentional exceptions are marked with @SuppressWarnings at the call site.
    // The upstream canary's lenient job disables it (-PlenientLint=true) so that new upstream
    // deprecations read as a migration heads-up there, not as a break.
    if (!providers.gradleProperty("lenientLint").map(String::toBoolean).getOrElse(false)) {
        options.compilerArgs.add("-Werror")
    }
}

// A way to run Playwright CLI commands using the Java source dependency.
// Particularly useful from within a CI context (see ./.github/workflows.gradle.yml).
// https://playwright.dev/docs/cli#install-system-dependencies
// https://github.com/microsoft/playwright-java/issues/571
// ./gradlew playwright --args="install-deps"
tasks.register<JavaExec>("playwright") {
    classpath(sourceSets["test"].runtimeClasspath)
    mainClass.set("com.microsoft.playwright.CLI")
}
