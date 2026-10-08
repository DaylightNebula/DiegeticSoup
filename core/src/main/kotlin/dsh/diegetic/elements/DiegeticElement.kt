package dsh.diegetic.elements

import dsh.diegetic.interaction.Hit
import dsh.diegetic.interaction.Interactive
import dsh.diegetic.interaction.Ray
import dsh.diegetic.interaction.RenderContext
import dsh.diegetic.interop.DItem
import net.kyori.adventure.text.Component
import org.joml.Matrix4f
import java.util.LinkedList

interface DiegeticElement {
    fun render(output: LinkedList<RenderedElement>, parent: Matrix4f)

    /**
     * Renders for a particular viewer or group of viewers. Elements with hover effects or other
     * per-viewer visuals override this; the default ignores the context.
     */
    fun render(output: LinkedList<RenderedElement>, parent: Matrix4f, context: RenderContext) = render(output, parent)

    /**
     * Finds the nearest interactive target hit by [ray], given in the same space [parent] maps this
     * element into. Elements without interactive parts return null.
     */
    fun hitTest(ray: Ray, parent: Matrix4f): Hit? = null
}

sealed class RenderedElement {
    abstract val entityId: Int
    abstract val offset: Matrix4f

    data class Item(override val entityId: Int, val item: DItem, override val offset: Matrix4f): RenderedElement()
    data class Text(
        override val entityId: Int,
        val text: Component,
        override val offset: Matrix4f,
        val options: TextDisplayOptions = TextDisplayOptions()
    ): RenderedElement()

    /**
     * An invisible interaction entity that lets players click [target]. [offset] maps the unit square
     * (-0.5..0.5 on x and y) onto the clickable rectangle; the controller places an axis-aligned hitbox
     * around it in the world.
     */
    data class Interaction(
        override val entityId: Int,
        override val offset: Matrix4f,
        val target: Interactive
    ): RenderedElement()
}

/**
 * Text display settings beyond the text itself.
 *
 * @param lineWidth       Width in font pixels at which the client wraps the text.
 * @param backgroundColor ARGB background colour, or null for the vanilla default background.
 * @param alignment       How lines are aligned against the widest line.
 */
data class TextDisplayOptions(
    val lineWidth: Int = DEFAULT_LINE_WIDTH,
    val backgroundColor: Int? = null,
    val alignment: TextAlignment = TextAlignment.CENTER
) {
    companion object {
        const val DEFAULT_LINE_WIDTH = 200
        const val DEFAULT_BACKGROUND = 0x40000000
    }

    /** The background colour that is actually sent, resolving null to the vanilla default. */
    val resolvedBackground: Int get() = backgroundColor ?: DEFAULT_BACKGROUND
}

/** Line alignment of a text display, sent as its alignment flags. */
enum class TextAlignment(val flags: Byte) {
    CENTER(0), LEFT(0x08), RIGHT(0x10)
}
