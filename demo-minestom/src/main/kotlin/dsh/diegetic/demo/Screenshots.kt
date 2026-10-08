package dsh.diegetic.demo

import dsh.diegetic.demo.stations.HudStation
import dsh.diegetic.demo.stations.WidgetsStation
import dsh.diegetic.flex.FlexElement
import dsh.diegetic.flex.FlexNode
import dsh.diegetic.flex.MinecraftFont
import dsh.diegetic.interaction.orientation
import dsh.diegetic.interop.MinestomLocation
import dsh.nebsclient.api.NebsClient
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import org.joml.Vector3f
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.system.exitProcess

/** Width of the saved screenshots; the client's high-DPI captures are scaled down to it. */
private const val IMAGE_WIDTH = 1280

/**
 * Starts the demo hub, drives a Minecraft client around it with NebsClient, and saves the README
 * screenshots to `docs/images`. Run with `./gradlew :demo-minestom:captureDemoScreenshots`.
 */
fun main() {
    val output = Path.of(System.getProperty("screenshots.output", "docs/images"))
    val home = Path.of(System.getProperty("nebs.home", ".nebs")).toAbsolutePath()
    val keepClient = System.getProperty("nebs.keepClient").toBoolean()
    Files.createDirectories(output)

    startDemoServer("all")
    Thread.sleep(1000)

    val client = NebsClient.running(home).firstOrNull() ?: NebsClient {
        this.home = home
        name = "DemoBot"
        quitOnClose = !keepClient
        readyTimeout = Duration.ofMinutes(15)
    }
    try {
        client.connect("localhost", 25565)
        client.setHudVisible(false)
        client.setWindowSize(960, 540)
        val player = MinecraftServer.getConnectionManager().onlinePlayers.first()
        onServer { player.isAllowFlying = true; player.isFlying = true }
        Thread.sleep(2000)
        val shots = Shots(client, player, output)

        // the whole hub from behind the spawn
        shots.take("hub", eye = Vec(0.5, 44.0, 14.0), target = Vec(0.5, 42.0, 0.0))

        // the HUD station is pictured from the player's view instead, below
        stations.filter { it != HudStation }.forEach { station ->
            val origin = stationOrigin(station)
            shots.take(station.id, eye = origin.add(station.cameraEye).asVec(), target = origin.add(station.cameraTarget).asVec())
        }

        // the counter after a few real clicks, with the button hovered
        val widgets = stationOrigin(WidgetsStation)
        val counter = WidgetsStation.clickerPanelElement
        val button = nodeWorldPoint(counter, WidgetsStation.clickerButton, widgets.add(2.6, 2.1, 0.0))
        shots.aim(eye = Vec(button.x.toDouble(), button.y.toDouble(), button.z + 3.0), target = button.toVec())
        repeat(3) { client.attack(); Thread.sleep(300) }
        shots.take("widgets-clicked", eye = Vec(button.x.toDouble(), button.y + 0.15, button.z + 2.6), target = button.toVec().add(0.0, 0.15, 0.0))

        // the HUD, seen from the player's own view
        onServer { HudStation.toggleHud(player) }
        val hud = stationOrigin(HudStation)
        shots.take("hud-view", eye = Vec(hud.x(), hud.y() + 1.8, hud.z() + 4.0), target = Vec(hud.x(), hud.y() + 1.8, hud.z()))
        onServer { HudStation.toggleHud(player) }

        println("Saved screenshots to ${output.toAbsolutePath()}")
    } finally {
        if (keepClient) client.disconnect() else client.close()
        MinecraftServer.stopCleanly()
    }
    exitProcess(0)
}

private fun Pos.asVec() = Vec(x, y, z)
private fun Vector3f.toVec() = Vec(x.toDouble(), y.toDouble(), z.toDouble())

/** Runs [block] on the server tick thread and waits for it. */
private fun <T> onServer(block: () -> T): T {
    val future = CompletableFuture<T>()
    MinecraftServer.getSchedulerManager().scheduleNextTick {
        try { future.complete(block()) } catch (e: Throwable) { future.completeExceptionally(e) }
    }
    return future.get(5, TimeUnit.SECONDS)
}

/** The world position of the centre of [node] in a flex panel shown at [origin]. */
private fun nodeWorldPoint(element: FlexElement, node: FlexNode<*>, origin: Pos): Vector3f {
    val boxes = element.layout()
    val root = boxes.getValue(element.root)
    val box = boxes.getValue(node)
    val blocksPerPixel = element.scale / MinecraftFont.PIXELS_PER_BLOCK
    return Vector3f(
        (box.x + box.width / 2 - element.anchor.x * root.width) * blocksPerPixel,
        (element.anchor.y * root.height - (box.y + box.height / 2)) * blocksPerPixel,
        0f
    ).rotate(MinestomLocation(origin).orientation()).add(origin.x.toFloat(), origin.y.toFloat(), origin.z.toFloat())
}

private class Shots(val client: NebsClient, val player: Player, val output: Path) {
    /** Puts the player's eye at [eye], facing [target], and waits until client and server agree. */
    fun aim(eye: Vec, target: Vec) {
        val d = target.sub(eye)
        val yaw = Math.toDegrees(atan2(-d.x(), d.z())).toFloat()
        val pitch = Math.toDegrees(-asin(d.y() / d.length())).toFloat()
        val feet = Pos(eye.x(), eye.y() - player.eyeHeight, eye.z(), yaw, pitch)
        onServer { player.isFlying = true; player.teleport(feet) }
        waitFor("the client to arrive") {
            val status = client.status()
            abs(status.x - feet.x()) < 0.1 && abs(status.y - feet.y()) < 0.1 && abs(status.z - feet.z()) < 0.1
        }
        client.lookAt(target.x(), target.y(), target.z())
        waitFor("the server to see the view") {
            val status = client.status()
            val diff = ((player.position.yaw - status.yaw.toFloat()) % 360f + 360f) % 360f
            minOf(diff, 360f - diff) < 0.5f && abs(player.position.pitch - status.pitch.toFloat()) < 0.5f
        }
    }

    /** Aims, lets animations and hovers settle, then saves a resized JPEG called [name]. */
    fun take(name: String, eye: Vec, target: Vec) {
        aim(eye, target)
        Thread.sleep(1500)
        val shot = client.screenshot("demo-$name").path
        save(ImageIO.read(shot.toFile()), output.resolve("$name.jpg"))
        println("Saved $name.jpg")
    }

    private fun save(source: BufferedImage, target: Path) {
        val height = source.height * IMAGE_WIDTH / source.width
        val scaled = BufferedImage(IMAGE_WIDTH, height, BufferedImage.TYPE_INT_RGB)
        scaled.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            drawImage(source, 0, 0, IMAGE_WIDTH, height, null)
            dispose()
        }
        val writer = ImageIO.getImageWritersByFormatName("jpg").next()
        val params = writer.defaultWriteParam.apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = 0.88f
        }
        ImageIO.createImageOutputStream(target.toFile()).use { stream ->
            writer.output = stream
            writer.write(null, IIOImage(scaled, null, null), params)
        }
        writer.dispose()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5000
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) throw IllegalStateException("Timed out waiting for $what")
            Thread.sleep(50)
        }
    }
}
