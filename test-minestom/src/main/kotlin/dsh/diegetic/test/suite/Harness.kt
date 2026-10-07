package dsh.diegetic.test.suite

import dsh.diegetic.DiegeticAPI
import dsh.nebsclient.api.EntityInfo
import dsh.nebsclient.api.NebsClient
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.abs

class AssertionFailed(message: String): RuntimeException(message)

fun check(condition: Boolean, message: () -> String) {
    if (!condition) throw AssertionFailed(message())
}

inline fun <reified T: Throwable> expectThrows(message: String, block: () -> Unit): T {
    try {
        block()
    } catch (e: Throwable) {
        if (e is T) return e
        throw AssertionFailed("$message: expected ${T::class.simpleName} but got $e")
    }
    throw AssertionFailed("$message: expected ${T::class.simpleName} but nothing was thrown")
}

/** Runs [block] on the server tick thread and waits for its result. */
fun <T> onServer(block: () -> T): T {
    val future = CompletableFuture<T>()
    MinecraftServer.getSchedulerManager().scheduleNextTick {
        try { future.complete(block()) } catch (e: Throwable) { future.completeExceptionally(e) }
    }
    return future.get(5, TimeUnit.SECONDS)
}

/** Everything a scenario needs: the client driving the game, its server-side player, and helpers. */
class TestContext(
    val client: NebsClient?,
    private val serverPlayer: Player?,
    private val screenshotDir: Path,
    private val scenarioName: String
) {
    val player: Player get() = serverPlayer ?: throw IllegalStateException("Scenario needs a client")
    val nebs: NebsClient get() = client ?: throw IllegalStateException("Scenario needs a client")

    /** Display entities the client currently knows about within [radius] of [at]. */
    fun displaysNear(at: Pos, radius: Double = 4.0): List<EntityInfo> = nebs.entities(64.0)
        .filter { it.type.endsWith("item_display") || it.type.endsWith("text_display") }
        .filter { it.distanceTo(at) <= radius }

    /** Polls until [predicate] holds for the displays near [at], or fails after [timeoutMs]. */
    fun awaitDisplays(
        at: Pos,
        description: String,
        radius: Double = 4.0,
        timeoutMs: Long = 5000,
        predicate: (List<EntityInfo>) -> Boolean
    ): List<EntityInfo> {
        val deadline = System.currentTimeMillis() + timeoutMs
        var last = displaysNear(at, radius)
        while (!predicate(last)) {
            if (System.currentTimeMillis() > deadline)
                throw AssertionFailed("Timed out waiting for $description; saw ${last.summary()}")
            Thread.sleep(100)
            last = displaysNear(at, radius)
        }
        return last
    }

    fun awaitCounts(at: Pos, items: Int, texts: Int, radius: Double = 4.0) =
        awaitDisplays(at, "$items item and $texts text displays", radius) {
            it.count { e -> e.type.endsWith("item_display") } == items &&
                it.count { e -> e.type.endsWith("text_display") } == texts
        }

    /** Takes a screenshot and copies it into the suite's output folder. */
    fun screenshot(label: String): Path {
        val info = nebs.screenshot("$scenarioName-$label")
        val target = screenshotDir.resolve("$scenarioName-$label.png")
        Files.copy(info.path, target, StandardCopyOption.REPLACE_EXISTING)
        return target
    }

    /**
     * Largest difference between a first screenshot and several later ones taken at uneven gaps.
     * Uneven gaps stop a periodic animation from lining up with screenshot latency and looking still.
     */
    fun maxAnimatedDifference(): Double {
        val first = screenshot("0")
        return listOf(130L, 210L, 340L).mapIndexed { index, gap ->
            Thread.sleep(gap)
            centralPixelDifference(first, screenshot("${index + 1}"))
        }.max().also { println("[suite]   max screenshot diff %.4f".format(it)) }
    }

    /** Places the player at [stand] and points the camera at [lookAt]. */
    fun standAndLook(stand: Pos, lookAt: Pos) {
        onServer { player.teleport(stand) }
        Thread.sleep(300)
        nebs.lookAt(lookAt.x, lookAt.y, lookAt.z)
        Thread.sleep(200)
    }

    fun removeAllControllers() = onServer {
        DiegeticAPI.get().getActiveControllers().toList().forEach { DiegeticAPI.get().removeController(it) }
    }
}

fun EntityInfo.distanceTo(pos: Pos): Double {
    val dx = x - pos.x; val dy = y - pos.y; val dz = z - pos.z
    return Math.sqrt(dx * dx + dy * dy + dz * dz)
}

fun List<EntityInfo>.summary() =
    if (isEmpty()) "none" else joinToString { "${it.type.substringAfter(':')}@(%.2f, %.2f, %.2f)".format(it.x, it.y, it.z) }

/**
 * Fraction of pixels in the central region of two screenshots whose colour differs noticeably.
 * [fraction] is the share of the width and height (around the centre) that is compared, so the
 * sky and far terrain don't dilute changes to a display in the middle of the screen.
 */
fun centralPixelDifference(a: Path, b: Path, fraction: Double = 0.25): Double {
    val imgA: BufferedImage = ImageIO.read(a.toFile())
    val imgB: BufferedImage = ImageIO.read(b.toFile())
    check(imgA.width == imgB.width && imgA.height == imgB.height) { "Screenshot sizes differ" }

    val halfW = (imgA.width * fraction / 2).toInt(); val halfH = (imgA.height * fraction / 2).toInt()
    val x0 = imgA.width / 2 - halfW; val x1 = imgA.width / 2 + halfW
    val y0 = imgA.height / 2 - halfH; val y1 = imgA.height / 2 + halfH
    var differing = 0
    var total = 0
    for (y in y0 until y1 step 2) for (x in x0 until x1 step 2) {
        val pa = imgA.getRGB(x, y); val pb = imgB.getRGB(x, y)
        val delta = abs((pa shr 16 and 0xff) - (pb shr 16 and 0xff)) +
            abs((pa shr 8 and 0xff) - (pb shr 8 and 0xff)) +
            abs((pa and 0xff) - (pb and 0xff))
        if (delta > 30) differing++
        total++
    }
    return differing.toDouble() / total
}
