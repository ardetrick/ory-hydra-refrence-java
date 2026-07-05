plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ory-hydra-reference"

include("reference-app")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            // Overwrites the catalog's testcontainersOryHydra version:
            // ./gradlew build -PtestcontainersOryHydraVersion=<version>
            // Relied on by .github/workflows/upstream-canary.yml in this repo AND by the
            // reference-app-compat workflow in ardetrick/testcontainers-ory-hydra — do not
            // remove without updating both.
            val override = providers.gradleProperty("testcontainersOryHydraVersion")
            if (override.isPresent) {
                version("testcontainersOryHydra", override.get())
            }
        }
    }
}
