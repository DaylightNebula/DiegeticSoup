package dsh.diegetic.test.suite

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.MinestomDiegeticAPI
import dsh.diegetic.test.startTemplateServer
import dsh.nebsclient.api.NebsClient
import net.minestom.server.MinecraftServer
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.system.exitProcess

/**
 * Runs every [Scenario] against a Minestom server with a real client driven by NebsClient.
 *
 * System properties:
 *  - `nebs.home`: nebs home holding the client install (default `./.nebs`)
 *  - `nebs.keepClient`: leave the client running afterwards to speed up the next run
 *  - `suite.only`: comma separated scenario names to run
 *  - `suite.screenshots`: where screenshots are copied
 */
fun main() = startTemplateServer {
    DiegeticAPI.set(MinestomDiegeticAPI().apply(MinestomDiegeticAPI::init))

    val home = Path.of(System.getProperty("nebs.home", ".nebs")).toAbsolutePath()
    val keepClient = System.getProperty("nebs.keepClient").toBoolean()
    val only = System.getProperty("suite.only")?.split(',')?.map(String::trim)?.toSet()
    val screenshotDir = Path.of(System.getProperty("suite.screenshots", "build/test-screenshots"))
    Files.createDirectories(screenshotDir)

    PacketRecorder.install()
    val selected = (scenarios + packetScenarios).filter { only == null || it.name in only }
    val results = mutableListOf<Pair<String, Throwable?>>()

    // scenarios without a client run first, so builder bugs show up before the slow launch
    selected.filterNot { it.needsClient }.forEach { scenario ->
        results += runScenario(scenario, TestContext(null, null, screenshotDir, scenario.name))
    }

    val inWorld = selected.filter { it.needsClient }
    var client: NebsClient? = null
    try {
        if (inWorld.isNotEmpty()) {
            println("[suite] Starting client in $home (the first launch downloads the game)")
            client = NebsClient.running(home).firstOrNull() ?: NebsClient {
                this.home = home
                name = "DiegeticTester"
                quitOnClose = !keepClient
                readyTimeout = Duration.ofMinutes(15)
            }
            client.connect("localhost", 25565)
            client.setHudVisible(false)
            client.setWindowSize(854, 480)

            val player = MinecraftServer.getConnectionManager().onlinePlayers.first()
            inWorld.forEach { scenario ->
                val context = TestContext(client, player, screenshotDir, scenario.name)
                results += runScenario(scenario, context)
                context.removeAllControllers()
                runCatching { context.awaitDisplays(ANCHOR, "cleanup", radius = 8.0) { it.isEmpty() } }
            }
        }
    } catch (e: Throwable) {
        results += "suite setup" to e
    } finally {
        client?.let { if (keepClient) it.disconnect() else it.close() }
    }

    val failed = results.filter { it.second != null }
    println()
    println("[suite] ${results.size - failed.size}/${results.size} passed; screenshots in ${screenshotDir.toAbsolutePath()}")
    failed.forEach { (name, error) -> println("[suite] FAILED $name: ${error?.message ?: error}") }

    MinecraftServer.stopCleanly()
    exitProcess(if (failed.isEmpty()) 0 else 1)
}

private fun runScenario(scenario: Scenario, context: TestContext): Pair<String, Throwable?> {
    val start = System.currentTimeMillis()
    val error = runCatching { scenario.body(context) }.exceptionOrNull()
    val time = System.currentTimeMillis() - start
    if (error == null) {
        println("[suite] PASS ${scenario.name} (${time}ms)")
    } else {
        println("[suite] FAIL ${scenario.name} (${time}ms): ${error.message}")
        if (error !is AssertionFailed) error.printStackTrace()
    }
    return scenario.name to error
}
