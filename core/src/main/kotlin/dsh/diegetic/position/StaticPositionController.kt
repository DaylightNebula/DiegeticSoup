package dsh.diegetic.position

import dsh.diegetic.interop.DLocation

/**
 * A position controller that always returns the given position.
 */
class StaticPositionController(
    private val position: DLocation
): PositionController {
    override fun getPosition() = position
}
