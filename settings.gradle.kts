plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "wumpus"
include("core", "console")
