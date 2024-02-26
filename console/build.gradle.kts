plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass = "io.github.wumpus.console.AppKt"
}

dependencies {
    implementation(project(":core"))
}

// Make the run task read the stdin
(tasks.run) {
    standardInput = System.`in`
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
