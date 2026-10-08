repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":minestom"))
    implementation("net.minestom:minestom:26_3-SNAPSHOT")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    // only for captureDemoScreenshots
    implementation("com.github.DaylightNebula.NebsClient:nebs-api:v0.3.2")
}

val demos = listOf("all", "basics", "flexbox", "widgets", "hud", "animation")

tasks.register<JavaExec>("runDemo") {
    group = "application"
    description = "Run a demo Minestom server on localhost:25565. Pick one with -Pdemo=${demos.joinToString("|")} (default all)."

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dsh.diegetic.demo.DemoServerKt")
    systemProperty("demo", findProperty("demo") ?: "all")
}

tasks.register<JavaExec>("captureDemoScreenshots") {
    group = "documentation"
    description = "Start the demo hub, drive a Minecraft client through it with NebsClient, and save the README screenshots."

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dsh.diegetic.demo.ScreenshotsKt")
    systemProperty("nebs.home", rootProject.layout.projectDirectory.dir(".nebs").asFile.absolutePath)
    systemProperty("screenshots.output", rootProject.layout.projectDirectory.dir("docs/images").asFile.absolutePath)
    findProperty("keepClient")?.let { systemProperty("nebs.keepClient", it.toString()) }
}
