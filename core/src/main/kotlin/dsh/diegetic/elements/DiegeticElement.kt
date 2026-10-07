package dsh.diegetic.elements

import dsh.diegetic.interop.DItem
import net.kyori.adventure.text.Component
import org.joml.Matrix4f
import java.util.LinkedList

interface DiegeticElement {
    fun render(output: LinkedList<RenderedElement>, parent: Matrix4f)
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
