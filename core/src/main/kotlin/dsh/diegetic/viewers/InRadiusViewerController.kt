package dsh.diegetic.viewers

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.interop.DLocation
import dsh.diegetic.interop.DPlayer

class InRadiusViewerController(
    private val center: DLocation,
    private val radius: Double
): ViewerController {
    override fun tick(
        currentViewers: Collection<DPlayer>,
        position: DLocation
    ): ViewerController.ViewerTickResult {
        return ViewerController.ViewerTickResult(
            toAdd = DiegeticAPI.get().getPlayersInWorld(center.world())
                .filter(::inRange)
                .filterNot(currentViewers::contains),
            toRemove = currentViewers.filterNot(::inRange)
        )
    }

    fun inRange(player: DPlayer) = player.location().distanceToSq(center) <= radius * radius
}