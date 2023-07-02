plugins {
    id("project.java-conventions")
    id("java-library")
}

dependencies {
    api(platform("org.testcontainers:testcontainers-bom:1.17.6"))
    api("org.testcontainers:testcontainers")
    api("org.testcontainers:junit-jupiter")
}
