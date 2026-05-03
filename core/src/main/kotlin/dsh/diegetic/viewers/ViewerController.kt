package dsh.diegetic.viewers

import dsh.diegetic.interop.DLocation
import dsh.diegetic.interop.DPlayer

interface ViewerController {

    /**
     * The result from `tick`.
     *
     * @param toAdd The players to add
     * @param toRemove The players to remove
     */
    data class ViewerTickResult(
        val toAdd: List<DPlayer>,
        val toRemove: List<DPlayer>,
        var removeController: Boolean = false
    )

    /**
     * Calculate what players to add and remove from the viewer controller.
     *
     * @param currentViewers The current viewers
     * @param position The position of the element
     */
    fun tick(currentViewers: Collection<DPlayer>, position: DLocation): ViewerTickResult
}