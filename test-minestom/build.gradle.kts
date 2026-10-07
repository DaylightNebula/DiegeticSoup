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
