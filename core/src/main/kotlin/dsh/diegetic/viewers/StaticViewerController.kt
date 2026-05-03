package dsh.diegetic.viewers

import dsh.diegetic.interop.DLocation
import dsh.diegetic.interop.DPlayer

/**
 * A viewer controller that always shows the given players.
 */
class StaticViewerController(
    private val viewers: List<DPlayer>
): ViewerController {
    override fun tick(
        currentViewers: Collection<DPlayer>,
        position: DLocation
    ): ViewerController.ViewerTickResult {
        return if (currentViewers.isEmpty() && viewers.isNotEmpty())
            ViewerController.ViewerTickResult(
                viewers,
                emptyList()
            )
        else ViewerController.ViewerTickResult(emptyList(), emptyList())
    }
}