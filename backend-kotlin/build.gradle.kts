plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    application
    id("io.ktor.plugin") version "3.0.1"
}

repositories { mavenCentral() }

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.0.1")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.1")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

application { mainClass.set("com.example.webapi.MainKt") }
kotlin { jvmToolchain(21) }
