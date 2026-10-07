package dsh.diegetic.test.suite

import dsh.diegetic.flex.*
import dsh.diegetic.interop.DItem

/** A placeholder item for layout-only tests; it is never rendered. */
object TestItem : DItem

/** A flex layout made only of containers and fixed-size items, so a browser can lay out the same tree. */
class FlexCase(val name: String, val root: FlexContainer)

fun box(w: Number? = null, h: Number? = null) = FlexItem.create(TestItem).apply {
    w?.let { width(it.toFloat()) }
    h?.let { height(it.toFloat()) }
}

/** Every node of a tree in pre-order, matching the order boxes are compared in. */
fun preOrder(node: FlexNode<*>): List<FlexNode<*>> =
    listOf(node) + if (node is FlexContainer) node.children.flatMap { preOrder(it) } else emptyList()

private fun row(width: Int? = null, height: Int? = null) = FlexContainer.row().apply {
    width?.let { width(it) }; height?.let { height(it) }
}

val flexCases: List<FlexCase> = buildList {
    // justify-content
    JustifyContent.entries.forEach { justify ->
        add(FlexCase("justify-${justify.name.lowercase()}",
            row(200, 40).justifyContent(justify).children(box(30, 20), box(40, 20), box(20, 20))))
    }
    // justify-content when items overflow
    listOf(JustifyContent.CENTER, JustifyContent.SPACE_AROUND, JustifyContent.SPACE_EVENLY, JustifyContent.FLEX_END).forEach { justify ->
        add(FlexCase("overflow-justify-${justify.name.lowercase()}",
            row(50, 20).justifyContent(justify).children(box(40, 20).shrink(0f), box(40, 20).shrink(0f))))
    }
    // align-items, with one item that can stretch
    AlignItems.entries.forEach { align ->
        add(FlexCase("align-items-${align.name.lowercase()}",
            row(200, 60).alignItems(align).children(box(30, 20), box(30, 35), box(30, null))))
    }
    add(FlexCase("align-self",
        row(200, 60).alignItems(AlignItems.FLEX_START).children(
            box(30, 20).alignSelf(AlignSelf.FLEX_END),
            box(30, 20).alignSelf(AlignSelf.CENTER),
            box(30, null).alignSelf(AlignSelf.STRETCH),
            box(30, 20))))
    // grow
    add(FlexCase("grow-ratios",
        row(300, 20).children(box(null, 20).basis(0f).grow(1f), box(null, 20).basis(0f).grow(2f), box(null, 20).basis(0f).grow(3f))))
    add(FlexCase("grow-from-width",
        row(300, 20).children(box(50, 20).grow(1f), box(100, 20).grow(1f), box(30, 20))))
    add(FlexCase("grow-fractional-sum",
        row(300, 20).children(box(50, 20).grow(0.2f), box(50, 20).grow(0.3f))))
    // shrink, weighted by base size
    add(FlexCase("shrink-weighted",
        row(100, 20).children(box(80, 20), box(40, 20).shrink(2f), box(30, 20).shrink(0f))))
    // min/max freezing
    add(FlexCase("grow-max-freeze",
        row(300, 20).children(box(null, 20).basis(0f).grow(1f).maxWidth(50f), box(null, 20).basis(0f).grow(1f), box(null, 20).basis(0f).grow(1f))))
    add(FlexCase("shrink-min-freeze",
        row(120, 20).children(box(100, 20).minWidth(90f), box(100, 20), box(100, 20))))
    // gap, padding, margin
    add(FlexCase("gap-padding-margin",
        row(200, null).padding(10f, 6f).gap(5).children(box(30, 20).margin(3), box(30, 20).margin(0f, 8f), box(30, 20))))
    // column direction
    add(FlexCase("column-center",
        FlexContainer.column().size(100, 200).justifyContent(JustifyContent.CENTER).alignItems(AlignItems.CENTER)
            .children(box(30, 20), box(60, 30), box(null, 20))))
    add(FlexCase("column-stretch-and-grow",
        FlexContainer.column().size(100, 200).gap(4).children(box(null, 20), box(null, null).grow(1f), box(40, 30).alignSelf(AlignSelf.FLEX_END))))
    // reverse directions
    add(FlexCase("row-reverse",
        FlexContainer.create().direction(FlexDirection.ROW_REVERSE).size(200, 30).gap(5).children(box(30, 20), box(40, 20).margin(0f, 4f, 0f, 0f), box(20, 20))))
    add(FlexCase("column-reverse",
        FlexContainer.create().direction(FlexDirection.COLUMN_REVERSE).size(60, 150).justifyContent(JustifyContent.CENTER).children(box(30, 20), box(40, 30))))
    // wrapping and align-content
    AlignContent.entries.forEach { align ->
        add(FlexCase("wrap-align-content-${align.name.lowercase()}",
            row(100, 120).wrap(FlexWrap.WRAP).gap(5).alignContent(align)
                .children(box(30, 20), box(30, 20), box(30, null), box(30, 20), box(30, 25))))
    }
    add(FlexCase("wrap-reverse",
        row(100, null).wrap(FlexWrap.WRAP_REVERSE).gap(4f, 6f).children(box(40, 20), box(40, 30), box(40, 20))))
    add(FlexCase("wrap-auto-height-grow",
        row(100, null).wrap(FlexWrap.WRAP).children(box(40, 20).grow(1f), box(40, 20).grow(1f), box(40, 20).grow(1f))))
    add(FlexCase("column-wrap",
        FlexContainer.column().height(70).wrap(FlexWrap.WRAP).gap(4).children(box(30, 30), box(20, 30), box(40, 30))))
    // percentages
    add(FlexCase("percent-sizes",
        row(200, 100).children(box(null, null).width(Length.percent(25f)).height(Length.percent(50f)), box(null, 20).width(Length.percent(50f)))))
    // nested auto-sized containers
    add(FlexCase("nested-auto",
        FlexContainer.column().padding(5).gap(3).children(
            FlexContainer.row().padding(4).gap(2).children(box(), box(), box()),
            box(50, 10),
            FlexContainer.row().justifyContent(JustifyContent.SPACE_BETWEEN).children(box(10, 10), box(10, 10)))))
    // automatic minimum size of nested containers
    add(FlexCase("auto-min-size",
        row(50, null).children(
            FlexContainer.row().children(box(), box()),
            FlexContainer.row().children(box(), box()))))
    add(FlexCase("nested-grow-in-column",
        FlexContainer.column().size(120, 100).children(
            FlexContainer.row().grow(1f).alignItems(AlignItems.CENTER).justifyContent(JustifyContent.SPACE_EVENLY).children(box(20, 20), box(20, 30)),
            FlexContainer.row().height(20).children(box(null, null).grow(1f), box(30, null)))))
}
