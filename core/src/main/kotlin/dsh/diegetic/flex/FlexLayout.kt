package dsh.diegetic.flex

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** A laid-out border box in text pixels, relative to the root's top-left corner, with y pointing down. */
data class Box(val x: Float, val y: Float, val width: Float, val height: Float)

/**
 * Lays out a flex tree following the CSS Flexible Box Layout algorithm (§9), simplified:
 * no `margin: auto`, `order`, baseline alignment, `aspect-ratio` or percentage padding.
 *
 * Text measures with [MinecraftFont]; items have a natural size of [FlexItem.DEFAULT_SIZE].
 */
class FlexLayout private constructor() {
    companion object {
        /** Lays out [root] at its own size (explicit, or shrink-to-fit its content). */
        @JvmStatic
        fun compute(root: FlexNode<*>): Map<FlexNode<*>, Box> = FlexLayout().run(root)
    }

    private val out = LinkedHashMap<FlexNode<*>, Box>()
    private val heightMemo = HashMap<Pair<FlexNode<*>, Float>, Float>()

    private fun run(root: FlexNode<*>): Map<FlexNode<*>, Box> {
        val width = clampWidth(root, root.style.width.resolve(null) ?: maxContentWidth(root), null)
        val height = root.style.height.resolve(null)?.let { clampHeight(root, it, null) } ?: heightForWidth(root, width, null)
        place(root, 0f, 0f, width, height)
        return out
    }

    // ---- placement ----

    private fun place(node: FlexNode<*>, x: Float, y: Float, width: Float, height: Float) {
        out[node] = Box(x, y, width, height)
        if (node is FlexContainer) layoutContainer(node, width, height, x, y, record = true)
    }

    // ---- intrinsic sizes (border boxes) ----

    private fun clampWidth(node: FlexNode<*>, value: Float, reference: Float?): Float {
        val minW = node.style.minWidth.resolve(reference) ?: 0f
        val maxW = node.style.maxWidth.resolve(reference) ?: Float.POSITIVE_INFINITY
        return max(minW, min(value, maxW))
    }

    private fun clampHeight(node: FlexNode<*>, value: Float, reference: Float?): Float {
        val minH = node.style.minHeight.resolve(reference) ?: 0f
        val maxH = node.style.maxHeight.resolve(reference) ?: Float.POSITIVE_INFINITY
        return max(minH, min(value, maxH))
    }

    /** Width the node would take with unlimited space: one line of text, children side by side. */
    private fun maxContentWidth(node: FlexNode<*>): Float {
        node.style.width.resolve(null)?.let { return clampWidth(node, it, null) }
        val content = when (node) {
            is FlexText -> MinecraftFont.boxWidth(MinecraftFont.measure(node.text))
            is FlexItem -> FlexItem.DEFAULT_SIZE
            is FlexContainer -> node.style.padding.horizontal + combineChildWidths(node, ::maxContentWidth, minContent = false)
        }
        return clampWidth(node, content, null)
    }

    /**
     * The narrowest the node can get without overflowing: the longest word, or the widest child.
     * With [ownSize] false the node's own width is ignored, giving the content size CSS uses for
     * a flex item's automatic minimum.
     */
    private fun minContentWidth(node: FlexNode<*>, ownSize: Boolean = true): Float {
        if (ownSize) node.style.width.resolve(null)?.let { return clampWidth(node, it, null) }
        val content = when (node) {
            is FlexText -> MinecraftFont.minContentWidth(node.text) + MinecraftFont.BACKGROUND_PADDING
            is FlexItem -> FlexItem.DEFAULT_SIZE
            is FlexContainer -> node.style.padding.horizontal + combineChildWidths(node, { minContentWidth(it) }, minContent = true)
        }
        return clampWidth(node, content, null)
    }

    private fun combineChildWidths(node: FlexContainer, measure: (FlexNode<*>) -> Float, minContent: Boolean): Float {
        val widths = node.children.map { measure(it) + it.style.margin.horizontal }
        if (widths.isEmpty()) return 0f
        val fixedHeight = node.style.height.resolve(null)
        if (!node.style.direction.isRow && node.style.wrap != FlexWrap.NO_WRAP && fixedHeight != null) {
            return wrappedColumnsWidth(node, widths, fixedHeight - node.style.padding.vertical)
        }
        val sideBySide = node.style.direction.isRow && !(minContent && node.style.wrap != FlexWrap.NO_WRAP)
        return if (sideBySide) widths.sum() + node.style.columnGap * (widths.size - 1) else widths.max()
    }

    /** Width of a wrapping column container: its children broken into columns that fit [innerHeight]. */
    private fun wrappedColumnsWidth(node: FlexContainer, widths: List<Float>, innerHeight: Float): Float {
        val columns = mutableListOf<Float>()
        var columnWidth = 0f
        var used = 0f
        node.children.forEachIndexed { i, child ->
            val outerHeight = heightForWidth(child, widths[i] - child.style.margin.horizontal, null) + child.style.margin.vertical
            val add = outerHeight + if (used > 0f) node.style.rowGap else 0f
            if (used > 0f && used + add > innerHeight + EPSILON) {
                columns += columnWidth
                columnWidth = 0f
                used = outerHeight
            } else used += add
            columnWidth = max(columnWidth, widths[i])
        }
        columns += columnWidth
        return columns.sum() + node.style.columnGap * (columns.size - 1)
    }

    /** Content-based border-box height at border-box [width], ignoring the node's own height property. */
    private fun contentHeight(node: FlexNode<*>, width: Float): Float = heightMemo.getOrPut(node to width) {
        when (node) {
            is FlexText -> MinecraftFont.measure(node.text, width - MinecraftFont.BACKGROUND_PADDING).height
            is FlexItem -> FlexItem.DEFAULT_SIZE
            is FlexContainer -> layoutContainer(node, width, null, 0f, 0f, record = false)
        }
    }

    /** Border-box height at border-box [width]: the explicit height, or the content height, clamped. */
    private fun heightForWidth(node: FlexNode<*>, width: Float, reference: Float?): Float {
        val value = node.style.height.resolve(reference) ?: contentHeight(node, width)
        return clampHeight(node, value, reference)
    }

    // ---- the flex algorithm ----

    /** Per-item state; margins are logical, so reversed axes start from the right or bottom. */
    private class FlexItemState(val node: FlexNode<*>, row: Boolean, mainReversed: Boolean, crossReversed: Boolean) {
        val margin = node.style.margin
        private val mainFirst = if (row) margin.left else margin.top
        private val mainLast = if (row) margin.right else margin.bottom
        private val crossFirst = if (row) margin.top else margin.left
        private val crossLast = if (row) margin.bottom else margin.right
        val mainMarginStart = if (mainReversed) mainLast else mainFirst
        val mainMarginEnd = if (mainReversed) mainFirst else mainLast
        val crossMarginStart = if (crossReversed) crossLast else crossFirst
        val crossMarginEnd = if (crossReversed) crossFirst else crossLast
        val mainMargins get() = mainMarginStart + mainMarginEnd
        val crossMargins get() = crossMarginStart + crossMarginEnd

        var base = 0f
        var hypothetical = 0f
        var minMain = 0f
        var maxMain = Float.POSITIVE_INFINITY
        var target = 0f
        var frozen = false
        var violation = 0f
        var cross = 0f
        var stretch = false
        var mainPos = 0f
        var crossPos = 0f

        val outerTarget get() = target + mainMargins
        val outerHypothetical get() = hypothetical + mainMargins
        val outerCross get() = cross + crossMargins
    }

    private class FlexLine(val items: List<FlexItemState>) {
        var crossSize = 0f
        var crossPos = 0f
    }

    private fun resolveAlign(item: FlexNode<*>, container: FlexContainer): AlignItems = when (item.style.alignSelf) {
        AlignSelf.AUTO -> container.style.alignItems
        AlignSelf.STRETCH -> AlignItems.STRETCH
        AlignSelf.FLEX_START -> AlignItems.FLEX_START
        AlignSelf.FLEX_END -> AlignItems.FLEX_END
        AlignSelf.CENTER -> AlignItems.CENTER
    }

    /**
     * Lays out [container]'s children inside its border box. [outerHeight] is null when the height
     * depends on content. Returns the container's border-box height; when [record] is set, children
     * are placed at absolute positions offset by ([originX], [originY]).
     */
    private fun layoutContainer(
        container: FlexContainer,
        outerWidth: Float,
        outerHeight: Float?,
        originX: Float,
        originY: Float,
        record: Boolean
    ): Float {
        val style = container.style
        val padding = style.padding
        val row = style.direction.isRow
        val innerWidth = max(0f, outerWidth - padding.horizontal)
        val innerHeight = outerHeight?.let { max(0f, it - padding.vertical) }
        val mainSize: Float? = if (row) innerWidth else innerHeight
        val crossSize: Float? = if (row) innerHeight else innerWidth
        val mainGap = if (row) style.columnGap else style.rowGap
        val crossGap = if (row) style.rowGap else style.columnGap

        // 1. flex base sizes and hypothetical main sizes
        val items = container.children.map {
            FlexItemState(it, row, style.direction.isReverse, style.wrap == FlexWrap.WRAP_REVERSE)
        }
        items.forEach { item ->
            val s = item.node.style
            item.stretch = resolveAlign(item.node, container) == AlignItems.STRETCH &&
                (if (row) s.height else s.width) == Length.Auto
            if (row) {
                val definite = s.width.resolve(innerWidth)
                item.base = s.flexBasis.resolve(innerWidth) ?: definite ?: maxContentWidth(item.node)
                val autoMin = min(definite ?: Float.POSITIVE_INFINITY, minContentWidth(item.node, ownSize = false))
                item.minMain = s.minWidth.resolve(innerWidth) ?: autoMin
                item.maxMain = s.maxWidth.resolve(innerWidth) ?: Float.POSITIVE_INFINITY
            } else {
                item.cross = columnItemWidth(item, innerWidth)
                val definite = s.height.resolve(innerHeight)
                val content = contentHeight(item.node, item.cross)
                item.base = s.flexBasis.resolve(innerHeight) ?: definite ?: content
                item.minMain = s.minHeight.resolve(innerHeight) ?: min(definite ?: Float.POSITIVE_INFINITY, content)
                item.maxMain = s.maxHeight.resolve(innerHeight) ?: Float.POSITIVE_INFINITY
            }
            item.hypothetical = clampMain(item, item.base)
        }

        // 2. collect into lines
        val lines = mutableListOf<FlexLine>()
        if (style.wrap == FlexWrap.NO_WRAP || mainSize == null) {
            lines += FlexLine(items)
        } else {
            var current = mutableListOf<FlexItemState>()
            var used = 0f
            items.forEach { item ->
                val add = item.outerHypothetical + if (current.isEmpty()) 0f else mainGap
                if (current.isNotEmpty() && used + add > mainSize + EPSILON) {
                    lines += FlexLine(current)
                    current = mutableListOf()
                    used = 0f
                    used += item.outerHypothetical
                } else used += add
                current += item
            }
            if (current.isNotEmpty() || lines.isEmpty()) lines += FlexLine(current)
        }

        // 3. resolve flexible lengths
        lines.forEach { line ->
            if (mainSize == null) line.items.forEach { it.target = it.hypothetical }
            else resolveFlexibleLengths(line.items, mainSize, mainGap)
        }

        // 4. cross sizes
        items.forEach { item ->
            val s = item.node.style
            if (row) {
                item.cross = s.height.resolve(innerHeight)?.let { clampHeight(item.node, it, innerHeight) }
                    ?: clampHeight(item.node, contentHeight(item.node, item.target), innerHeight)
            }
            // column items already have their width from step 1
        }
        val singleLineDefinite = style.wrap == FlexWrap.NO_WRAP && crossSize != null
        lines.forEach { line ->
            line.crossSize = if (singleLineDefinite) crossSize!! else line.items.maxOfOrNull { it.outerCross } ?: 0f
        }
        val linesCross = lines.sumOf { it.crossSize.toDouble() }.toFloat() + crossGap * (lines.size - 1)
        val containerCross = crossSize ?: linesCross
        var crossFree = containerCross - linesCross
        if (!singleLineDefinite && crossSize != null && style.alignContent == AlignContent.STRETCH && crossFree > 0 && lines.isNotEmpty()) {
            lines.forEach { it.crossSize += crossFree / lines.size }
            crossFree = 0f
        }
        lines.forEach { line ->
            line.items.forEach { item ->
                if (item.stretch) {
                    val stretched = line.crossSize - item.crossMargins
                    item.cross = if (row) clampHeight(item.node, stretched, innerHeight) else clampWidth(item.node, stretched, innerWidth)
                }
            }
        }

        // 5. main-axis alignment
        val effectiveMain = mainSize ?: lines.maxOfOrNull { line ->
            line.items.sumOf { it.outerTarget.toDouble() }.toFloat() + mainGap * (line.items.size - 1)
        } ?: 0f
        lines.forEach { line ->
            val used = line.items.sumOf { it.outerTarget.toDouble() }.toFloat() + mainGap * (line.items.size - 1)
            val (start, between) = distribute(style.justifyContent.toDistribution(), effectiveMain - used, line.items.size)
            var pos = start
            line.items.forEach { item ->
                item.mainPos = pos + item.mainMarginStart
                pos += item.outerTarget + mainGap + between
            }
        }

        // 6. cross-axis alignment of lines, then items within lines
        val (lineStart, lineBetween) = if (style.wrap == FlexWrap.NO_WRAP) 0f to 0f
            else distribute(style.alignContent.toDistribution(), crossFree, lines.size)
        var linePos = lineStart
        lines.forEach { line ->
            line.crossPos = linePos
            linePos += line.crossSize + crossGap + lineBetween
            line.items.forEach { item ->
                val offset = when (resolveAlign(item.node, container)) {
                    AlignItems.FLEX_END -> line.crossSize - item.outerCross
                    AlignItems.CENTER -> (line.crossSize - item.outerCross) / 2f
                    AlignItems.FLEX_START, AlignItems.STRETCH -> 0f
                }
                item.crossPos = line.crossPos + offset + item.crossMarginStart
            }
        }

        // 7. reverse directions, then convert to boxes
        val contentMain = if (mainSize != null) mainSize else effectiveMain
        if (record) {
            items.forEach { item ->
                val main = if (style.direction.isReverse) contentMain - item.mainPos - item.target else item.mainPos
                val cross = if (style.wrap == FlexWrap.WRAP_REVERSE) containerCross - item.crossPos - item.cross else item.crossPos
                val x = originX + padding.left + if (row) main else cross
                val y = originY + padding.top + if (row) cross else main
                val w = if (row) item.target else item.cross
                val h = if (row) item.cross else item.target
                place(item.node, x, y, w, h)
            }
        }

        return outerHeight ?: (padding.vertical + if (row) containerCross else effectiveMain)
    }

    /** Width of a child of a column container: explicit, stretched, or fit-content. */
    private fun columnItemWidth(item: FlexItemState, innerWidth: Float): Float {
        val s = item.node.style
        s.width.resolve(innerWidth)?.let { return clampWidth(item.node, it, innerWidth) }
        val available = innerWidth - item.crossMargins
        if (item.stretch) return clampWidth(item.node, available, innerWidth)
        val fit = min(maxContentWidth(item.node), max(minContentWidth(item.node), available))
        return clampWidth(item.node, fit, innerWidth)
    }

    private fun clampMain(item: FlexItemState, value: Float) = max(item.minMain, min(value, item.maxMain)).coerceAtLeast(0f)

    /** CSS §9.7: grow or shrink unfrozen items until the line fills [mainSize] or every item hits a limit. */
    private fun resolveFlexibleLengths(items: List<FlexItemState>, mainSize: Float, gap: Float) {
        val gaps = gap * (items.size - 1).coerceAtLeast(0)
        val growing = items.sumOf { it.outerHypothetical.toDouble() }.toFloat() + gaps < mainSize
        fun factor(item: FlexItemState) = if (growing) item.node.style.flexGrow else item.node.style.flexShrink

        items.forEach { item ->
            item.frozen = false
            item.target = item.hypothetical
            if (factor(item) == 0f || (growing && item.base > item.hypothetical) || (!growing && item.base < item.hypothetical)) {
                item.frozen = true
            }
        }
        fun freeSpace() = mainSize - gaps - items.sumOf {
            (if (it.frozen) it.outerTarget else it.base + it.mainMargins).toDouble()
        }.toFloat()
        val initialFree = freeSpace()

        while (items.any { !it.frozen }) {
            val unfrozen = items.filter { !it.frozen }
            var free = freeSpace()
            val factorSum = unfrozen.sumOf { factor(it).toDouble() }.toFloat()
            if (factorSum < 1f) {
                val scaled = initialFree * factorSum
                if (abs(scaled) < abs(free)) free = scaled
            }

            if (growing) {
                unfrozen.forEach { it.target = it.base + if (factorSum > 0f) free * it.node.style.flexGrow / factorSum else 0f }
            } else {
                val scaledSum = unfrozen.sumOf { (it.node.style.flexShrink * it.base).toDouble() }.toFloat()
                unfrozen.forEach {
                    val ratio = if (scaledSum > 0f) it.node.style.flexShrink * it.base / scaledSum else 0f
                    it.target = it.base + free * ratio
                }
            }

            var totalViolation = 0f
            unfrozen.forEach {
                val clamped = clampMain(it, it.target)
                it.violation = clamped - it.target
                it.target = clamped
                totalViolation += it.violation
            }
            when {
                abs(totalViolation) < EPSILON -> unfrozen.forEach { it.frozen = true }
                totalViolation > 0f -> unfrozen.filter { it.violation > 0f }.forEach { it.frozen = true }
                else -> unfrozen.filter { it.violation < 0f }.forEach { it.frozen = true }
            }
        }
    }

    private enum class Distribution { START, END, CENTER, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY }

    private fun JustifyContent.toDistribution() = when (this) {
        JustifyContent.FLEX_START -> Distribution.START
        JustifyContent.FLEX_END -> Distribution.END
        JustifyContent.CENTER -> Distribution.CENTER
        JustifyContent.SPACE_BETWEEN -> Distribution.SPACE_BETWEEN
        JustifyContent.SPACE_AROUND -> Distribution.SPACE_AROUND
        JustifyContent.SPACE_EVENLY -> Distribution.SPACE_EVENLY
    }

    private fun AlignContent.toDistribution() = when (this) {
        AlignContent.FLEX_START, AlignContent.STRETCH -> Distribution.START
        AlignContent.FLEX_END -> Distribution.END
        AlignContent.CENTER -> Distribution.CENTER
        AlignContent.SPACE_BETWEEN -> Distribution.SPACE_BETWEEN
        AlignContent.SPACE_AROUND -> Distribution.SPACE_AROUND
        AlignContent.SPACE_EVENLY -> Distribution.SPACE_EVENLY
    }

    /** Offset of the first item and extra space between items for [free] leftover space over [count] items. */
    private fun distribute(distribution: Distribution, free: Float, count: Int): Pair<Float, Float> {
        if (count == 0) return 0f to 0f
        return when (distribution) {
            Distribution.START -> 0f to 0f
            Distribution.END -> free to 0f
            Distribution.CENTER -> free / 2f to 0f
            Distribution.SPACE_BETWEEN ->
                if (free > 0f && count > 1) 0f to free / (count - 1) else 0f to 0f
            // distributed alignment falls back to flex-start when items overflow, as browsers do
            Distribution.SPACE_AROUND ->
                if (free > 0f) free / count / 2f to free / count else 0f to 0f
            Distribution.SPACE_EVENLY ->
                if (free > 0f) free / (count + 1) to free / (count + 1) else 0f to 0f
        }
    }
}

private const val EPSILON = 0.001f
