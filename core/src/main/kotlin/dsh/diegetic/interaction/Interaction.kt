package dsh.diegetic.interaction

import dsh.diegetic.controller.DiegeticController
import dsh.diegetic.interop.DPlayer

/** Which mouse button a player clicked with. */
enum class ClickType { LEFT, RIGHT }

/** How hover highlights are shown when several players watch the same UI. */
enum class HoverMode {
    /** Each player only sees what they themselves are looking at highlighted. */
    PER_VIEWER,
    /** Everyone sees the same highlights: anything any viewer is looking at. */
    SHARED
}

/**
 * A click on an interactive part of a UI.
 *
 * @param x       Where the click landed, from the left edge of the clicked element, in its own units
 *                (text pixels for flex layouts), or null when it couldn't be worked out precisely.
 * @param y       Like [x], from the top edge.
 * @param width   Width of the clicked element in the same units.
 * @param height  Height of the clicked element in the same units.
 * @param isDrag  True for the repeats sent while the right button is held on the same element.
 */
class ClickEvent(
    val player: DPlayer,
    val type: ClickType,
    val controller: DiegeticController,
    val x: Float?,
    val y: Float?,
    val width: Float,
    val height: Float,
    val isDrag: Boolean
) {
    val isLeft: Boolean get() = type == ClickType.LEFT
    val isRight: Boolean get() = type == ClickType.RIGHT
}

/** Something in a UI that players can click and hover. */
interface Interactive {
    /** Disabled targets still block clicks to what is behind them, but ignore them. */
    val interactionEnabled: Boolean get() = true

    fun click(event: ClickEvent)

    /** Called instead of [click] for the repeats sent while the right button is held on this target. */
    fun drag(event: ClickEvent) {}
}

/** The result of a ray hitting an interactive target; positions are as in [ClickEvent]. */
class Hit(
    val distance: Float,
    val target: Interactive,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/**
 * What a render is for: the viewer it is personalised for (null when shared by several viewers),
 * and the targets that should be drawn hovered.
 */
class RenderContext(val viewer: DPlayer?, val hovered: Set<Interactive>) {
    fun isHovered(target: Interactive): Boolean = target in hovered

    companion object {
        @JvmField val NONE = RenderContext(null, emptySet())
    }
}
