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

// The images embedded in the README are the screenshots the functional tests capture on every
// run. This task republishes them into docs/images so they can be regenerated on purpose instead
// of drifting (the previous set dated from 2022). Keys are docs/images/<flow> directory names,
// values the producing test's screenshot directory under build/test-results/screenshots.
val readmeScreenshotFlows =
    mapOf(
        "full-oauth-flow-oidc" to "completeFullOAuthFlowUsingUIToLogin",
        "remember-me" to "skipConsentScreenOnSecondLoginWhenRememberMeIsUsed",
    )

tasks.register("refreshReadmeScreenshots") {
    group = "documentation"
    description = "Reruns the functional tests and copies their screenshots into docs/images."
    dependsOn(tasks.test)
    val screenshotsDir = layout.buildDirectory.dir("test-results/screenshots")
    val docsImagesDir =
        rootProject.layout.projectDirectory
            .dir("docs/images")
            .asFile
    doLast {
        readmeScreenshotFlows.forEach { (flow, testName) ->
            val source = screenshotsDir.get().dir(testName).asFile
            require(source.isDirectory && !source.listFiles().isNullOrEmpty()) {
                "No screenshots at $source — did the test get renamed without updating this mapping?"
            }
            val target = docsImagesDir.resolve(flow)
            target.deleteRecursively()
            source.copyRecursively(target)
        }
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
