plugins {
    id("xyz.jpenilla.run-paper") version "3.1.0"
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
    compileOnly("io.papermc.paper:paper-api:26.2.build.121-stable")
    implementation("com.github.retrooper:packetevents-spigot:2.13.0")
}

tasks {
    runServer {
        minecraftVersion("26.2")

        downloadPlugins {
            hangar("ViaVersion", "5.12.0")
        }
    }
}
