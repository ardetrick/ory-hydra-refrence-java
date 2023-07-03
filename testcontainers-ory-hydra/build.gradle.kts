plugins {
    id("java-library")
    id("project.java-conventions")
}

dependencies {
    api(platform("org.testcontainers:testcontainers-bom:1.17.6"))
    api("org.testcontainers:testcontainers")
    api("org.testcontainers:junit-jupiter")
}
