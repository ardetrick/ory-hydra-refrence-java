plugins {
    id("org.springframework.boot") version "3.5.3"
    id("project.java-conventions")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-freemarker")
    implementation("org.springframework.boot:spring-boot-starter-jersey")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.session:spring-session-core")
    implementation("sh.ory.hydra:hydra-client:2.2.1")

    testImplementation("com.ardetrick.testcontainers:testcontainers-ory-hydra:0.0.2")
    testImplementation("com.auth0:java-jwt:4.5.0")
    testImplementation("com.microsoft.playwright:playwright:1.53.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint")
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
