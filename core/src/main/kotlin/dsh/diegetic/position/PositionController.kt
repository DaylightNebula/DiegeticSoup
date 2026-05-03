package dsh.diegetic.position

import dsh.diegetic.interop.DLocation

interface PositionController {
    /**
     * Calculate the position of the element.
     */
    fun getPosition(): DLocation
}
