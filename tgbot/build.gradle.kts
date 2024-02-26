plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass = "io.github.wumpus.tgbot.AppKt"
}

dependencies {
    implementation(project(":core"))

    implementation("dev.inmo:tgbotapi:10.1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("com.github.ajalt.clikt:clikt:4.2.2")

    implementation("org.slf4j:slf4j-simple:2.0.12")
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.3")
}

// Make 'jar' command create executable JAR with dependencies
tasks.withType<Jar> {
    manifest {
        attributes("Main-Class" to application.mainClass)
    }
    from(
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    )
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
