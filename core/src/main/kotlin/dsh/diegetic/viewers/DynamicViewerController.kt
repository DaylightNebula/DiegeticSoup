package dsh.diegetic.viewers

import dsh.diegetic.interop.DLocation
import dsh.diegetic.interop.DPlayer

class DynamicViewerController(
    private val callback: (Collection<DPlayer>, position: DLocation) -> ViewerController.ViewerTickResult
): ViewerController {
    override fun tick(
        currentViewers: Collection<DPlayer>,
        position: DLocation
    ): ViewerController.ViewerTickResult = callback(currentViewers, position)
}