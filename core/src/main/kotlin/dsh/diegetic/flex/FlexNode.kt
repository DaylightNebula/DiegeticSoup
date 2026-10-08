package dsh.diegetic.flex

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.DiegeticDsl
import dsh.diegetic.elements.TextAlignment
import dsh.diegetic.interaction.RenderContext
import dsh.diegetic.interop.DItem
import net.kyori.adventure.text.Component

/**
 * A node in a flex layout: a box ([FlexContainer] or an interactive widget), [FlexText], [FlexItem]
 * or [FlexSlider].
 * Setters are fluent and return the concrete node type, so Java builder chains keep their type.
 */
@DiegeticDsl
sealed class FlexNode<T : FlexNode<T>> {
    val style = FlexStyle()

    @Suppress("UNCHECKED_CAST")
    protected val self: T get() = this as T

    fun width(px: Float): T { style.width = Length.Px(px); return self }
    fun width(px: Int): T = width(px.toFloat())
    fun width(length: Length): T { style.width = length; return self }
    fun height(px: Float): T { style.height = Length.Px(px); return self }
    fun height(px: Int): T = height(px.toFloat())
    fun height(length: Length): T { style.height = length; return self }
    fun size(width: Float, height: Float): T = width(width).height(height)
    fun size(width: Int, height: Int): T = width(width).height(height)

    fun minWidth(length: Length): T { style.minWidth = length; return self }
    fun minWidth(px: Float): T = minWidth(Length.Px(px))
    fun minHeight(length: Length): T { style.minHeight = length; return self }
    fun minHeight(px: Float): T = minHeight(Length.Px(px))
    fun maxWidth(length: Length): T { style.maxWidth = length; return self }
    fun maxWidth(px: Float): T = maxWidth(Length.Px(px))
    fun maxHeight(length: Length): T { style.maxHeight = length; return self }
    fun maxHeight(px: Float): T = maxHeight(Length.Px(px))

    fun grow(factor: Float): T { style.flexGrow = factor; return self }
    fun shrink(factor: Float): T { style.flexShrink = factor; return self }
    fun basis(length: Length): T { style.flexBasis = length; return self }
    fun basis(px: Float): T = basis(Length.Px(px))
    /** Like CSS `flex: <grow> <shrink> <basis>`. */
    fun flex(grow: Float, shrink: Float, basis: Length): T = grow(grow).shrink(shrink).basis(basis)

    fun alignSelf(align: AlignSelf): T { style.alignSelf = align; return self }

    fun margin(all: Float): T { style.margin = Edges.all(all); return self }
    fun margin(all: Int): T = margin(all.toFloat())
    fun margin(vertical: Float, horizontal: Float): T { style.margin = Edges.symmetric(vertical, horizontal); return self }
    fun margin(top: Float, right: Float, bottom: Float, left: Float): T { style.margin = Edges(top, right, bottom, left); return self }
}

/**
 * A box that lays out its children with flexbox, optionally drawing a background colour behind them.
 * [FlexContainer] is the plain version; interactive widgets such as [FlexButton] are boxes too.
 */
sealed class FlexBox<T : FlexBox<T>> : FlexNode<T>() {
    private val childNodes = mutableListOf<FlexNode<*>>()
    val children: List<FlexNode<*>> get() = childNodes

    /** ARGB background colour, or null for no background. */
    var background: Int? = null
        private set

    /** Entity ID of the background display, allocated on first use. */
    val backgroundEntityId by lazy { DiegeticAPI.get().nextEntityID() }

    /** The background to draw in [context]; widgets change it for hover, selection and so on. */
    internal open fun resolvedBackground(context: RenderContext): Int? = background

    fun child(node: FlexNode<*>): T { childNodes.add(node); return self }
    fun children(vararg nodes: FlexNode<*>): T { childNodes.addAll(nodes); return self }
    fun removeChild(node: FlexNode<*>): T { childNodes.remove(node); return self }
    fun clearChildren(): T { childNodes.clear(); return self }
    internal fun insertChild(index: Int, node: FlexNode<*>) { childNodes.add(index, node) }

    fun background(argb: Int?): T { background = argb; return self }
    fun background(argb: Long): T = background(argb.toInt())

    fun direction(direction: FlexDirection): T { style.direction = direction; return self }
    fun wrap(wrap: FlexWrap): T { style.wrap = wrap; return self }
    fun justifyContent(justify: JustifyContent): T { style.justifyContent = justify; return self }
    fun alignItems(align: AlignItems): T { style.alignItems = align; return self }
    fun alignContent(align: AlignContent): T { style.alignContent = align; return self }

    /** Like CSS `gap`: the same gap between rows and columns. */
    fun gap(px: Float): T { style.rowGap = px; style.columnGap = px; return self }
    fun gap(px: Int): T = gap(px.toFloat())
    /** Like CSS `gap: <row> <column>`. */
    fun gap(row: Float, column: Float): T { style.rowGap = row; style.columnGap = column; return self }
    fun rowGap(px: Float): T { style.rowGap = px; return self }
    fun columnGap(px: Float): T { style.columnGap = px; return self }

    fun padding(all: Float): T { style.padding = Edges.all(all); return self }
    fun padding(all: Int): T = padding(all.toFloat())
    fun padding(vertical: Float, horizontal: Float): T { style.padding = Edges.symmetric(vertical, horizontal); return self }
    fun padding(top: Float, right: Float, bottom: Float, left: Float): T { style.padding = Edges(top, right, bottom, left); return self }
}

/** A plain flex box: lays out its children, optionally drawing a background colour behind them. */
class FlexContainer : FlexBox<FlexContainer>() {
    companion object {
        @JvmStatic fun create() = FlexContainer()
        @JvmStatic fun row() = FlexContainer().direction(FlexDirection.ROW)
        @JvmStatic fun column() = FlexContainer().direction(FlexDirection.COLUMN)
    }
}

/** Text that wraps at its box width. Its natural size comes from the Minecraft default font. */
class FlexText(text: Component) : FlexNode<FlexText>() {
    companion object {
        @JvmStatic fun create(text: Component) = FlexText(text)
        @JvmStatic fun create(text: String) = FlexText(Component.text(text))
    }

    var text: Component = text
        private set

    /** ARGB background drawn behind just this text; transparent by default. */
    var textBackground: Int = 0
        private set

    /** Like CSS `text-align`: lines align against each other, and the text block within its box. */
    var textAlign: TextAlignment = TextAlignment.LEFT
        private set

    /** Entity ID of this node's display, allocated on first use. */
    val entityId by lazy { DiegeticAPI.get().nextEntityID() }

    fun text(text: Component): FlexText { this.text = text; return this }
    fun text(text: String): FlexText = text(Component.text(text))
    fun textBackground(argb: Int): FlexText { textBackground = argb; return this }
    fun textBackground(argb: Long): FlexText = textBackground(argb.toInt())
    fun textAlign(align: TextAlignment): FlexText { textAlign = align; return this }
}

/** An item display, drawn centred and scaled to fit its box. Its natural size is 16×16 px. */
class FlexItem(item: DItem) : FlexNode<FlexItem>() {
    companion object {
        const val DEFAULT_SIZE = 16f
        @JvmStatic fun create(item: DItem) = FlexItem(item)
    }

    var item: DItem = item
        private set

    /** Entity ID of this node's display, allocated on first use. */
    val entityId by lazy { DiegeticAPI.get().nextEntityID() }

    fun item(item: DItem): FlexItem { this.item = item; return this }
}
