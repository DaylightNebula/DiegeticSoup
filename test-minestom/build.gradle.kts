import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicReference

dependencies {
    implementation(project(":core"))
    implementation(project(":minestom"))
    implementation("org.joml:joml:1.10.8")
    implementation("net.minestom:minestom:2026.09.12-26.2")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
}

tasks.register<JavaExec>("runMinestom") {
    group = "application"
    description = "Run the Minestom Test Server"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dsh.diegetic.test.MainKt")
}

tasks.register<JavaExec>("runMinestomWithClient") {
    group = "application"
    description = "Run the Minestom Test Server and auto-launch a client"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dsh.diegetic.test.MainKt")

    val serverPort = 25565
    val clientCommand = listOf(
        "flatpak", "run", "org.prismlauncher.PrismLauncher",
        "--launch", "26.2",
        "--server", "localhost:$serverPort"
    )
    val clientProc = AtomicReference<Process?>()

    fun portOpen(): Boolean = try {
        Socket().use {
            it.connect(InetSocketAddress("127.0.0.1", serverPort), 500)
        }
        true
    } catch (e: IOException) {
        false
    }

    fun stopClient() {
        clientProc.getAndSet(null)?.let { p ->
            p.descendants().forEach { it.destroyForcibly() }
            p.destroyForcibly()
        }
        // Fallback: the game JVM runs inside the Flatpak sandbox and may not
        // be a child of our process. This also closes the Prism launcher window.
        ProcessBuilder("flatpak", "kill", "org.prismlauncher.PrismLauncher")
            .inheritIO().start().waitFor()
    }

    doFirst {
        Thread {
            // 1. wait for the server to start listening (max 60s)
            val deadline = System.currentTimeMillis() + 60_000
            while (!portOpen() && System.currentTimeMillis() < deadline) {
                Thread.sleep(500)
            }
            if (!portOpen()) return@Thread

            // 2. launch the client
            clientProc.set(ProcessBuilder(clientCommand).inheritIO().start())

            // 3. when the port closes, the server is gone -> stop the client
            while (portOpen()) Thread.sleep(1000)
            stopClient()
        }.apply { isDaemon = true; start() }
    }

    doLast { stopClient() }
}
