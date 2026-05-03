package dsh.diegetic.viewers

import dsh.diegetic.interop.DLocation
import dsh.diegetic.interop.DPlayer

class SinglePlayerNearbyViewerController(
    val player: DPlayer,
    val location: DLocation,
    val radius: Double
): ViewerController {
    override fun tick(
        currentViewers: Collection<DPlayer>,
        position: DLocation
    ): ViewerController.ViewerTickResult {
        val toAdd = if (currentViewers.isEmpty()) listOf(player) else emptyList()
        val removeNow = player.location().distanceToSq(location) > radius * radius
        return ViewerController.ViewerTickResult(toAdd, emptyList(), removeNow)
    }
}