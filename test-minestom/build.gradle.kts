repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":minestom"))
    implementation("org.joml:joml:1.10.8")
    implementation("net.minestom:minestom:26_3-SNAPSHOT")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("com.github.DaylightNebula.NebsClient:nebs-api:v0.3.2")
}

tasks.register<JavaExec>("runMinestom") {
    group = "application"
    description = "Run the Minestom Test Server"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dsh.diegetic.test.MainKt")
}

tasks.register<JavaExec>("runMinestomTests") {
    group = "verification"
    description = "Run the diegetic scenario suite against a Minestom server with a NebsClient client"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dsh.diegetic.test.suite.SuiteMainKt")

    systemProperty("nebs.home", rootProject.layout.projectDirectory.dir(".nebs").asFile.absolutePath)
    systemProperty("suite.screenshots", layout.buildDirectory.dir("test-screenshots").get().asFile.absolutePath)
    findProperty("keepClient")?.let { systemProperty("nebs.keepClient", it.toString()) }
    findProperty("only")?.let { systemProperty("suite.only", it.toString()) }
    findProperty("skipClient")?.let { systemProperty("suite.skipClient", it.toString()) }
}
