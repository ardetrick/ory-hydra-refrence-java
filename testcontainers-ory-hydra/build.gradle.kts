plugins {
    id("java-library")
    id("project.java-conventions")
}

dependencies {
    api(platform("org.testcontainers:testcontainers-bom:1.19.1"))
    api("org.testcontainers:testcontainers")
    api("org.testcontainers:junit-jupiter")
}
