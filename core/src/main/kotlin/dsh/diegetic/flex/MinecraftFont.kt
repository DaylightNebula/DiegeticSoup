package dsh.diegetic.flex

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextDecoration
import kotlin.math.floor

/**
 * Measures text the way a text display renders it with the vanilla default font, and holds the
 * display geometry the flex renderer relies on. All values are in text pixels (1/40 block at scale 1).
 *
 * The geometry was measured in game against a reference block:
 *  - a text display's background spans x from -w/2 to w/2 + 1 and y from 0 up to lines * [LINE_HEIGHT],
 *    where w is the widest line, so its bottom edge sits on the entity origin
 *  - an item display draws a block item as a [ITEM_BLOCK_SIZE] cube centred on the origin
 *  - empty text draws no background at all, so backgrounds use a single space
 */
object MinecraftFont {
    const val LINE_HEIGHT = 10f
    /** Extra width a text display's background adds beyond the text. */
    const val BACKGROUND_PADDING = 1f
    /** Pixels per block at scale 1 for both text and items. */
    const val PIXELS_PER_BLOCK = 40f
    const val ITEM_BLOCK_SIZE = 40f
    const val DEFAULT_ADVANCE = 6

    /** Advance widths (glyph + 1 px spacing) of the default font's ASCII glyphs that differ from 6. */
    private val advances: Map<Char, Int> = buildMap {
        put(' ', 4)
        "!',.:;|i".forEach { put(it, 2) }
        "`l".forEach { put(it, 3) }
        "\"()*I[]t{}".forEach { put(it, 4) }
        "<>fk".forEach { put(it, 5) }
        "@~".forEach { put(it, 7) }
    }

    fun advance(char: Char, bold: Boolean): Int = (advances[char] ?: DEFAULT_ADVANCE) + if (bold) 1 else 0

    private data class Glyph(val char: Char, val width: Int)

    /** Flattens a component to glyphs, tracking inherited bold. Non-text components are estimated from their key. */
    private fun glyphs(component: Component, inheritedBold: Boolean = false, out: MutableList<Glyph> = mutableListOf()): List<Glyph> {
        val bold = when (component.decoration(TextDecoration.BOLD)) {
            TextDecoration.State.TRUE -> true
            TextDecoration.State.FALSE -> false
            else -> inheritedBold
        }
        val content = when (component) {
            is TextComponent -> component.content()
            is net.kyori.adventure.text.TranslatableComponent -> component.fallback() ?: component.key()
            else -> ""
        }
        content.forEach { out.add(Glyph(it, advance(it, bold))) }
        component.children().forEach { glyphs(it, bold, out) }
        return out
    }

    /** The result of measuring text: the widest line and how many lines there are. */
    data class TextMetrics(val width: Float, val lines: Int) {
        val height: Float get() = lines * LINE_HEIGHT
    }

    /**
     * Measures [component], wrapping lines wider than [maxWidth] the way the client does: break at
     * the last space that fits (dropping it), or inside a word when no space fits.
     */
    fun measure(component: Component, maxWidth: Float? = null): TextMetrics {
        val lines = wrap(glyphs(component), maxWidth?.let { floor(it).toInt() })
        return TextMetrics(lines.maxOrNull()?.toFloat() ?: 0f, lines.size.coerceAtLeast(1))
    }

    /** Width of the widest unbreakable word: the narrowest the text can get without breaking words. */
    fun minContentWidth(component: Component): Float {
        var widest = 0
        var current = 0
        glyphs(component).forEach { g ->
            if (g.char == ' ' || g.char == '\n') current = 0 else { current += g.width; widest = maxOf(widest, current) }
        }
        return widest.toFloat()
    }

    /** Line widths after wrapping. */
    private fun wrap(glyphs: List<Glyph>, maxWidth: Int?): List<Int> {
        val lines = mutableListOf<Int>()
        var i = 0
        var lineStart = 0
        var width = 0
        var lastSpace = -1
        var widthAtLastSpace = 0

        fun newLine(lineWidth: Int, nextStart: Int) {
            lines.add(lineWidth)
            i = nextStart; lineStart = nextStart; width = 0; lastSpace = -1
        }

        while (i < glyphs.size) {
            val g = glyphs[i]
            when {
                g.char == '\n' -> newLine(width, i + 1)

                // a visible glyph overflows: break at the last space, or before this glyph
                maxWidth != null && g.char != ' ' && i > lineStart && width + g.width > maxWidth ->
                    if (lastSpace >= lineStart) newLine(widthAtLastSpace, lastSpace + 1) else newLine(width, i)

                else -> {
                    if (g.char == ' ') { lastSpace = i; widthAtLastSpace = width }
                    width += g.width
                    i++
                }
            }
        }
        lines.add(width)
        return lines
    }

    /** Width of a text node's box for [metrics]: the background is one pixel wider than the text. */
    fun boxWidth(metrics: TextMetrics) = metrics.width + BACKGROUND_PADDING
}
