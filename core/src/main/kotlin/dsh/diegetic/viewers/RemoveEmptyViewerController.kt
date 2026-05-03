package dsh.diegetic.viewers

import dsh.diegetic.interop.DLocation
import dsh.diegetic.interop.DPlayer

class RemoveEmptyViewerController(
    private val child: ViewerController
): ViewerController {
    override fun tick(
        currentViewers: Collection<DPlayer>,
        position: DLocation
    ): ViewerController.ViewerTickResult {
        val result = child.tick(currentViewers, position)
        val amount = currentViewers.size + result.toAdd.size - result.toRemove.size
        if (amount <= 0) result.removeController = true
        return result
    }
}
