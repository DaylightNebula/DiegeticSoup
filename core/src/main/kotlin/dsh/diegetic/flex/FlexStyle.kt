package dsh.diegetic.flex

/** The main axis and its direction, like CSS `flex-direction`. */
enum class FlexDirection {
    ROW, COLUMN, ROW_REVERSE, COLUMN_REVERSE;

    val isRow: Boolean get() = this == ROW || this == ROW_REVERSE
    val isReverse: Boolean get() = this == ROW_REVERSE || this == COLUMN_REVERSE
}

/** Like CSS `flex-wrap`. */
enum class FlexWrap { NO_WRAP, WRAP, WRAP_REVERSE }

/** Like CSS `justify-content`. */
enum class JustifyContent { FLEX_START, FLEX_END, CENTER, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY }

/** Like CSS `align-items`. Baseline alignment is not supported. */
enum class AlignItems { STRETCH, FLEX_START, FLEX_END, CENTER }

/** Like CSS `align-self`; [AUTO] uses the container's [AlignItems]. */
enum class AlignSelf { AUTO, STRETCH, FLEX_START, FLEX_END, CENTER }

/** Like CSS `align-content`. */
enum class AlignContent { STRETCH, FLEX_START, FLEX_END, CENTER, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY }

/** Which point of the root box sits on the element's origin. [x] and [y] are fractions from the top-left. */
enum class Anchor(val x: Float, val y: Float) {
    TOP_LEFT(0f, 0f), TOP_CENTER(0.5f, 0f), TOP_RIGHT(1f, 0f),
    CENTER_LEFT(0f, 0.5f), CENTER(0.5f, 0.5f), CENTER_RIGHT(1f, 0.5f),
    BOTTOM_LEFT(0f, 1f), BOTTOM_CENTER(0.5f, 1f), BOTTOM_RIGHT(1f, 1f)
}

/** A CSS-like length in text pixels (1/40 block at scale 1). */
sealed class Length {
    data object Auto : Length()
    data class Px(val value: Float) : Length()
    data class Percent(val value: Float) : Length()

    /** Resolves against [reference] (the parent's inner size); null when auto or the reference is unknown. */
    fun resolve(reference: Float?): Float? = when (this) {
        Auto -> null
        is Px -> value
        is Percent -> reference?.let { it * value / 100f }
    }

    companion object {
        @JvmField val AUTO: Length = Auto
        @JvmStatic fun px(value: Float): Length = Px(value)
        @JvmStatic fun percent(value: Float): Length = Percent(value)
    }
}

/** Sizes on each side of a box, like CSS margin and padding shorthands. */
data class Edges(val top: Float, val right: Float, val bottom: Float, val left: Float) {
    val horizontal: Float get() = left + right
    val vertical: Float get() = top + bottom

    companion object {
        @JvmField val ZERO = Edges(0f, 0f, 0f, 0f)
        @JvmStatic fun all(value: Float) = Edges(value, value, value, value)
        @JvmStatic fun symmetric(vertical: Float, horizontal: Float) = Edges(vertical, horizontal, vertical, horizontal)
    }
}

/** Every flexbox property of a node. Container properties are ignored on text and item nodes. */
class FlexStyle {
    // sizing
    var width: Length = Length.Auto
    var height: Length = Length.Auto
    var minWidth: Length = Length.Auto
    var minHeight: Length = Length.Auto
    var maxWidth: Length = Length.Auto
    var maxHeight: Length = Length.Auto

    // as a flex item
    var flexGrow: Float = 0f
    var flexShrink: Float = 1f
    var flexBasis: Length = Length.Auto
    var alignSelf: AlignSelf = AlignSelf.AUTO
    var margin: Edges = Edges.ZERO

    // as a flex container
    var direction: FlexDirection = FlexDirection.ROW
    var wrap: FlexWrap = FlexWrap.NO_WRAP
    var justifyContent: JustifyContent = JustifyContent.FLEX_START
    var alignItems: AlignItems = AlignItems.STRETCH
    var alignContent: AlignContent = AlignContent.STRETCH
    var rowGap: Float = 0f
    var columnGap: Float = 0f
    var padding: Edges = Edges.ZERO
}
