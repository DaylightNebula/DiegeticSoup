plugins {
    kotlin("jvm") version "2.3.21"
}

kotlin {
    jvmToolchain(25)
}

allprojects {
    apply(plugin = "kotlin")

    group = "dsh.diegetic"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://repo.codemc.io/repository/maven-releases/")
        maven("https://repo.codemc.io/repository/maven-snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://jitpack.io")
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }
}