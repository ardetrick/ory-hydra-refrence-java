plugins {
    id("java-library")
    id("project.java-conventions")
}

dependencies {
    api(platform("org.testcontainers:testcontainers-bom:1.18.3"))
    api("org.testcontainers:testcontainers")
    api("org.testcontainers:junit-jupiter")
}


