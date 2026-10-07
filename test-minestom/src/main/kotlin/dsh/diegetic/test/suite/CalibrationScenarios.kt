package dsh.diegetic.test.suite

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.controller.DSLController
import dsh.diegetic.elements.DiegeticElement
import dsh.diegetic.elements.RenderedElement
import dsh.diegetic.elements.TextDisplayOptions
import dsh.diegetic.interop.MinestomItem
import dsh.diegetic.interop.MinestomLocation
import dsh.diegetic.interop.MinestomPlayer
import dsh.diegetic.position.StaticPositionController
import dsh.diegetic.viewers.StaticViewerController
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.joml.Matrix4f
import java.nio.file.Path
import java.util.LinkedList
import javax.imageio.ImageIO

/** Emits fixed rendered elements, for measuring raw display geometry. */
class RawElement(private val elements: List<RenderedElement>): DiegeticElement {
    override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f) {
        elements.forEach { output.add(it) }
    }
}

data class PixelBox(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int)

/** Bounding box of pixels for which [match] holds. */
fun findPixels(path: Path, match: (r: Int, g: Int, b: Int) -> Boolean): PixelBox? {
    val img = ImageIO.read(path.toFile())
    var minX = Int.MAX_VALUE; var minY = Int.MAX_VALUE; var maxX = -1; var maxY = -1
    for (y in 0 until img.height) for (x in 0 until img.width) {
        val p = img.getRGB(x, y)
        if (match(p shr 16 and 0xff, p shr 8 and 0xff, p and 0xff)) {
            if (x < minX) minX = x; if (y < minY) minY = y
            if (x > maxX) maxX = x; if (y > maxY) maxY = y
        }
    }
    return if (maxX < 0) null else PixelBox(minX, minY, maxX, maxY)
}

val isMagenta = { r: Int, g: Int, b: Int -> r > 80 && b > 80 && g * 2 < minOf(r, b) }
val isRedish = { r: Int, g: Int, b: Int -> r > 40 && r > 2 * g && r > 2 * b }

/** The reference block's front face: the brightest red pixels, so the darker side face is excluded. */
fun findReferenceFace(path: Path): PixelBox? {
    val img = ImageIO.read(path.toFile())
    var maxR = 0
    for (y in 0 until img.height) for (x in 0 until img.width) {
        val p = img.getRGB(x, y)
        val r = p shr 16 and 0xff
        if (isRedish(r, p shr 8 and 0xff, p and 0xff) && r > maxR) maxR = r
    }
    if (maxR == 0) return null
    return findPixels(path) { r, g, b -> isRedish(r, g, b) && r >= maxR * 0.85 }
}

const val MAGENTA = 0xFFFF00FF.toInt()

/** Displays are anchored here; the reference block's front face lies on the same plane (z = 4). */
val CAL_ORIGIN = Pos(0.5, 41.5, 4.0)
/** A red concrete block whose front (south) face spans x 2..3, y 41..42 on the plane z = 4. */
const val REF_X = 2; const val REF_Y = 41; const val REF_Z = 3

/** A measured on-screen box in font pixels relative to [CAL_ORIGIN], with y pointing up. */
data class Measured(val left: Double, val right: Double, val bottom: Double, val top: Double) {
    val width get() = right - left
    val height get() = top - bottom
    override fun toString() = "x %.2f..%.2f (w %.2f)  y(up) %.2f..%.2f (h %.2f)".format(left, right, width, bottom, top, height)
}

/**
 * Spawns [element] at [CAL_ORIGIN], screenshots it next to the reference block, and measures the
 * magenta region in font pixels (1/40 block at [pxScale] 1) relative to the origin.
 */
fun TestContext.measureMagenta(label: String, element: DiegeticElement, pxScale: Float = 1f): Measured? {
    onServer {
        player.instance.setBlock(REF_X, REF_Y, REF_Z, net.minestom.server.instance.block.Block.RED_CONCRETE)
        player.teleport(Pos(1.0, 40.0, 8.0, 180f, 0f))
    }
    Thread.sleep(300)
    nebs.look(180f, 0f)
    val controller = onServer {
        DSLController.create()
            .viewerController(StaticViewerController(listOf(MinestomPlayer(player))))
            .positionController(StaticPositionController(MinestomLocation(CAL_ORIGIN)))
            .element(element)
            .interpolationDuration(0)
            .spawn()
    }
    Thread.sleep(1200)
    val shot = screenshot(label)
    onServer {
        DiegeticAPI.get().removeController(controller)
        player.instance.setBlock(REF_X, REF_Y, REF_Z, net.minestom.server.instance.block.Block.AIR)
    }
    Thread.sleep(300)

    val ref = findReferenceFace(shot) ?: throw AssertionFailed("$label: reference block not found")
    val box = findPixels(shot, isMagenta) ?: return null

    // pixels per block from the reference face, and the reference's bottom-left corner in world space
    val ppbX = (ref.maxX + 1 - ref.minX).toDouble()
    val ppbY = (ref.maxY + 1 - ref.minY).toDouble()
    fun worldX(sx: Double) = REF_X + (sx - ref.minX) / ppbX
    fun worldY(sy: Double) = REF_Y + (ref.maxY + 1 - sy) / ppbY

    val unit = 1.0 / 40.0 * pxScale
    return Measured(
        left = (worldX(box.minX.toDouble()) - CAL_ORIGIN.x) / unit,
        right = (worldX(box.maxX + 1.0) - CAL_ORIGIN.x) / unit,
        bottom = (worldY(box.maxY + 1.0) - CAL_ORIGIN.y) / unit,
        top = (worldY(box.minY.toDouble()) - CAL_ORIGIN.y) / unit,
    )
}

fun TestContext.measureMagenta(label: String, elements: List<RenderedElement>, pxScale: Float): String =
    "$label: ${measureMagenta(label, RawElement(elements), pxScale) ?: "not found"}"

val calibrationScenarios = listOf(
    Scenario("calibrate-display-geometry") {
        onServer { player.instance.setBlock(REF_X, REF_Y, REF_Z, net.minestom.server.instance.block.Block.RED_CONCRETE) }
        Thread.sleep(1500)
        fun bg(sx: Float, sy: Float, sz: Float) = listOf(RenderedElement.Text(DiegeticAPI.get().nextEntityID(),
            Component.text(" "), Matrix4f().scale(sx, sy, sz), TextDisplayOptions(200, MAGENTA)))
        fun item(sx: Float, sy: Float, sz: Float) = listOf(RenderedElement.Item(DiegeticAPI.get().nextEntityID(),
            MinestomItem(ItemStack.of(Material.MAGENTA_CONCRETE)), Matrix4f().scale(sx, sy, sz)))
        for ((sx, sy, sz) in listOf(Triple(1f, 1f, 1f), Triple(2f, 2f, 1f), Triple(2f, 2f, 2f), Triple(2f, 2f, 4f), Triple(1f, 1f, 4f))) {
            println("[cal] " + measureMagenta("bg-$sx-$sy-$sz", bg(sx, sy, sz), 1f))
        }
        for ((sx, sy, sz) in listOf(Triple(0.5f, 0.5f, 0.5f), Triple(1f, 1f, 1f), Triple(0.5f, 0.5f, 0.01f))) {
            println("[cal] " + measureMagenta("item-$sx-$sy-$sz", item(sx, sy, sz), 1f))
        }
    },
)

/** Writes the flex test cases as an HTML page for regenerating the CSS golden data in a browser. */
val flexExportScenario = Scenario("flex-export-css-cases", needsClient = false) {
    val path = java.nio.file.Path.of(System.getProperty("suite.screenshots", "build/test-screenshots")).resolveSibling("flex-cases.html")
    java.nio.file.Files.writeString(path, FlexHtml.page())
    println("[suite]   wrote ${path.toAbsolutePath()}")
}
