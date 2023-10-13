plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("io.github.wumpus.tgbot.AppKt")
}

dependencies {
    implementation(project(":core"))

    implementation("dev.inmo:tgbotapi:9.2.1")

    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
}