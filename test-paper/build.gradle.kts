plugins {
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.github.goooler.shadow") version "8.1.8"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    gradlePluginPortal()
}

dependencies {
    implementation(project(":core"))
    implementation(project(":paper"))
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    implementation("com.github.retrooper:packetevents-spigot:2.12.1")
}

tasks {
    runServer {
        minecraftVersion("1.20.4")

        downloadPlugins {
            hangar("ViaVersion", "5.9.0")
        }
    }
}
