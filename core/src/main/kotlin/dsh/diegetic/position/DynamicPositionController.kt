package dsh.diegetic.position

import dsh.diegetic.interop.DLocation

class DynamicPositionController(
    private val callback: () -> DLocation
): PositionController {
    override fun getPosition(): DLocation = callback()
}