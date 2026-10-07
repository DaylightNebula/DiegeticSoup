package dsh.diegetic.flex

import dsh.diegetic.DiegeticDsl
import dsh.diegetic.elements.DiegeticElement
import dsh.diegetic.elements.RenderedElement
import dsh.diegetic.elements.TextAlignment
import dsh.diegetic.elements.TextDisplayOptions
import net.kyori.adventure.text.Component
import org.joml.Matrix4f
import java.util.LinkedList
import kotlin.math.floor
import kotlin.math.min

/**
 * Renders a flex tree as display entities. The tree is laid out again on every render, so changing
 * text, items or styles reflows the UI; the controller then only sends packets for what moved or changed.
 *
 * The panel lies in the element's local XY plane facing +z (the side text displays render on).
 * Layout pixels are 1/40 block, multiplied by [scale].
 */
@DiegeticDsl
class FlexElement private constructor(val root: FlexNode<*>) : DiegeticElement {
    companion object {
        /** The background of a single space: 5 px wide (x -2..3) and one line tall. */
        private const val SPACE_BACKGROUND_WIDTH = 5f
        private const val SPACE_BACKGROUND_LEFT = 2f
        private val SPACE: Component = Component.text(" ")

        @JvmStatic fun create(root: FlexNode<*>) = FlexElement(root)
    }

    var anchor: Anchor = Anchor.CENTER
        private set
    var scale: Float = 1f
        private set
    /** Distance in blocks between nesting levels, so children draw in front of their container's background. */
    var depthStep: Float = 0.005f
        private set

    fun anchor(anchor: Anchor): FlexElement { this.anchor = anchor; return this }
    fun scale(scale: Float): FlexElement { this.scale = scale; return this }
    fun depthStep(blocks: Float): FlexElement { depthStep = blocks; return this }

    /** Computes the layout without rendering, e.g. to inspect the panel's size. */
    fun layout(): Map<FlexNode<*>, Box> = FlexLayout.compute(root)

    override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f) {
        val boxes = layout()
        val rootBox = boxes.getValue(root)
        Emitter(boxes, rootBox, parent, output).emit(root, 0)
    }

    private inner class Emitter(
        val boxes: Map<FlexNode<*>, Box>,
        val rootBox: Box,
        val parent: Matrix4f,
        val output: LinkedList<RenderedElement>
    ) {
        val blocksPerPixel = scale / MinecraftFont.PIXELS_PER_BLOCK

        fun worldX(px: Float) = (px - anchor.x * rootBox.width) * blocksPerPixel
        fun worldY(py: Float) = (anchor.y * rootBox.height - py) * blocksPerPixel

        fun emit(node: FlexNode<*>, depth: Int) {
            val box = boxes[node] ?: return
            val z = depth * depthStep
            when (node) {
                is FlexContainer -> {
                    node.background?.let { color -> emitBackground(node, box, z, color) }
                    node.children.forEach { emit(it, depth + 1) }
                }
                is FlexText -> emitText(node, box, z)
                is FlexItem -> emitItem(node, box, z)
            }
        }

        fun emitBackground(node: FlexContainer, box: Box, z: Float, color: Int) {
            if (box.width <= 0f || box.height <= 0f) return
            val sx = box.width / SPACE_BACKGROUND_WIDTH
            val sy = box.height / MinecraftFont.LINE_HEIGHT
            val offset = Matrix4f(parent)
                .translate(worldX(box.x + SPACE_BACKGROUND_LEFT * sx), worldY(box.y + box.height), z)
                .scale(sx * scale, sy * scale, scale)
            output.add(RenderedElement.Text(node.backgroundEntityId, SPACE, offset, TextDisplayOptions(backgroundColor = color)))
        }

        fun emitText(node: FlexText, box: Box, z: Float) {
            val maxWidth = floor(box.width - MinecraftFont.BACKGROUND_PADDING).coerceAtLeast(1f)
            val metrics = MinecraftFont.measure(node.text, maxWidth)
            // place the text block within a box that may be wider than it, like CSS text-align
            val slack = box.width - MinecraftFont.boxWidth(metrics)
            val left = box.x + when (node.textAlign) {
                TextAlignment.LEFT -> 0f
                TextAlignment.CENTER -> slack / 2f
                TextAlignment.RIGHT -> slack
            }
            // the display's background spans -w/2..w/2+1 and grows up from the origin
            val offset = Matrix4f(parent)
                .translate(worldX(left + metrics.width / 2f), worldY(box.y + metrics.height), z)
                .scale(scale)
            output.add(RenderedElement.Text(node.entityId, node.text, offset,
                TextDisplayOptions(lineWidth = maxWidth.toInt(), backgroundColor = node.textBackground, alignment = node.textAlign)))
        }

        fun emitItem(node: FlexItem, box: Box, z: Float) {
            val size = min(box.width, box.height)
            if (size <= 0f) return
            val offset = Matrix4f(parent)
                .translate(worldX(box.x + box.width / 2f), worldY(box.y + box.height / 2f), z)
                .scale(size / MinecraftFont.ITEM_BLOCK_SIZE * scale)
            output.add(RenderedElement.Item(node.entityId, node.item, offset))
        }
    }
}
