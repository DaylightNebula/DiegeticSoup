plugins {
    kotlin("jvm") version "2.3.21"
}

kotlin {
    jvmToolchain(25)
}

// The library modules published to Maven (and JitPack), as diegetic-<module>.
val publishedModules = setOf("core", "minestom", "paper")

allprojects {
    apply(plugin = "kotlin")

    // The version lives in gradle.properties so releases can override it: -Pversion=1.2.3
    // The group can be overridden too (-Pdiegetic.group=...), which JitPack builds use; see jitpack.yml.
    group = providers.gradleProperty("diegetic.group").getOrElse("dsh.diegetic")

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

configure(subprojects.filter { it.name in publishedModules }) {
    apply(plugin = "maven-publish")

    java {
        withSourcesJar()
    }

    tasks.jar {
        archiveBaseName.set("diegetic-${project.name}")
    }
    tasks.named<Jar>("sourcesJar") {
        archiveBaseName.set("diegetic-${project.name}")
    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                artifactId = "diegetic-${project.name}"
                from(components["java"])
                pom {
                    name.set("Diegetic Soup ${project.name}")
                    description.set("Diegetic (in-world) UIs for Minestom and Paper Minecraft servers")
                    url.set("https://github.com/DaylightNebula/DiegeticSoup")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                }
            }
        }
    }
}

// Installs the Claude Code skill from this checkout: into <project>/.claude/skills, or ~/.claude/skills with -Puser=true.
tasks.register<Copy>("installClaudeSkill") {
    group = "help"
    description = "Install the diegetic-soup Claude Code skill into .claude/skills (or ~/.claude/skills with -Puser=true)."
    val user = providers.gradleProperty("user").map { it.toBoolean() }.getOrElse(false)
    from(layout.projectDirectory.dir("claude-plugin/skills"))
    into(if (user) file("${System.getProperty("user.home")}/.claude/skills") else layout.projectDirectory.dir(".claude/skills"))
}
