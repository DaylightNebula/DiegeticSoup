package dsh.diegetic.flex

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.DiegeticDsl
import dsh.diegetic.elements.TextAlignment
import dsh.diegetic.interop.DItem
import net.kyori.adventure.text.Component

/**
 * A node in a flex layout: a [FlexContainer], [FlexText] or [FlexItem].
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

/** A box that lays out its children with flexbox, optionally drawing a background colour behind them. */
class FlexContainer : FlexNode<FlexContainer>() {
    companion object {
        @JvmStatic fun create() = FlexContainer()
        @JvmStatic fun row() = FlexContainer().direction(FlexDirection.ROW)
        @JvmStatic fun column() = FlexContainer().direction(FlexDirection.COLUMN)
    }

    private val childNodes = mutableListOf<FlexNode<*>>()
    val children: List<FlexNode<*>> get() = childNodes

    /** ARGB background colour, or null for no background. */
    var background: Int? = null
        private set

    /** Entity ID of the background display, allocated on first use. */
    val backgroundEntityId by lazy { DiegeticAPI.get().nextEntityID() }

    fun child(node: FlexNode<*>): FlexContainer { childNodes.add(node); return this }
    fun children(vararg nodes: FlexNode<*>): FlexContainer { childNodes.addAll(nodes); return this }
    fun removeChild(node: FlexNode<*>): FlexContainer { childNodes.remove(node); return this }
    fun clearChildren(): FlexContainer { childNodes.clear(); return this }

    fun background(argb: Int?): FlexContainer { background = argb; return this }
    fun background(argb: Long): FlexContainer = background(argb.toInt())

    fun direction(direction: FlexDirection): FlexContainer { style.direction = direction; return this }
    fun wrap(wrap: FlexWrap): FlexContainer { style.wrap = wrap; return this }
    fun justifyContent(justify: JustifyContent): FlexContainer { style.justifyContent = justify; return this }
    fun alignItems(align: AlignItems): FlexContainer { style.alignItems = align; return this }
    fun alignContent(align: AlignContent): FlexContainer { style.alignContent = align; return this }

    /** Like CSS `gap`: the same gap between rows and columns. */
    fun gap(px: Float): FlexContainer { style.rowGap = px; style.columnGap = px; return this }
    fun gap(px: Int): FlexContainer = gap(px.toFloat())
    /** Like CSS `gap: <row> <column>`. */
    fun gap(row: Float, column: Float): FlexContainer { style.rowGap = row; style.columnGap = column; return this }
    fun rowGap(px: Float): FlexContainer { style.rowGap = px; return this }
    fun columnGap(px: Float): FlexContainer { style.columnGap = px; return this }

    fun padding(all: Float): FlexContainer { style.padding = Edges.all(all); return this }
    fun padding(all: Int): FlexContainer = padding(all.toFloat())
    fun padding(vertical: Float, horizontal: Float): FlexContainer { style.padding = Edges.symmetric(vertical, horizontal); return this }
    fun padding(top: Float, right: Float, bottom: Float, left: Float): FlexContainer { style.padding = Edges(top, right, bottom, left); return this }
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
