// Share the root project's version catalog with buildSrc so plugin-jar versions live in
// gradle/libs.versions.toml alongside everything else.
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
