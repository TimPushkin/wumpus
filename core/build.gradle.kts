plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.10"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
