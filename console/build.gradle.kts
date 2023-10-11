plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("io.github.wumpus.console.AppKt")
}

dependencies {
    implementation(project(":core"))
}

// Make the run task read the stdin
(tasks.run) {
    standardInput = System.`in`
}
