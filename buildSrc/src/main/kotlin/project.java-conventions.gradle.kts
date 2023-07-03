plugins {
    id("java")
    id("io.freefair.lombok")
    id("io.spring.dependency-management")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
//        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}
