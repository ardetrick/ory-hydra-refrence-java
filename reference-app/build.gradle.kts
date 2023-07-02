plugins {
    id("org.springframework.boot") version "3.1.0"
    id("project.java-conventions")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-freemarker")
    implementation("org.springframework.boot:spring-boot-starter-jersey")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.session:spring-session-core")
    implementation("sh.ory.hydra:hydra-client:2.1.1")

    testImplementation(project(":testcontainers-ory-hydra"))
    testImplementation("com.auth0:java-jwt:4.2.1")
    testImplementation("com.microsoft.playwright:playwright:1.35.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// A way to run Playwright CLI commands using the Java source dependency.
// Particularly useful from within a CI context (see ./.github/workflows.gradle.yml).
// https://playwright.dev/docs/cli#install-system-dependencies
// https://github.com/microsoft/playwright-java/issues/571
// ./gradlew playwright -Pargs="install-deps"
tasks.register<JavaExec>("playwright") {
    classpath(sourceSets["test"].runtimeClasspath)
    mainClass.set("com.microsoft.playwright.CLI")
    val playwrightArgs = "args"
    if (project.hasProperty(playwrightArgs)) {
        args = project.findProperty(playwrightArgs)
            .toString()
            .split(" ")
    }
}
