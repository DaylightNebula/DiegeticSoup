package dsh.diegetic

import dsh.diegetic.interop.DPlayer

interface DiegeticAPI {
    companion object {
        var instance: DiegeticAPI? = null

        fun set(instance: DiegeticAPI) {
            DiegeticAPI.instance = instance
        }

        fun get(): DiegeticAPI = instance
            ?: throw IllegalStateException("DiegeticAPI is not initialized!")
    }

    /**
     * Returns the next entity ID to can be used.
     */
    fun nextEntityID(): Int

    /**
     * Returns the packet API associated with this API.
     */
    fun getPacketAPI(): PacketAPI

    /**
     * Returns a stream of all players in a given world.
     */
    fun getPlayersInWorld(world: String): Collection<DPlayer>

    /**
     * Returns all active controllers.
     */
    fun getActiveControllers(): Collection<DiegeticController>

    /**
     * Adds an active controller.
     */
    fun addController(controller: DiegeticController)

    /**
     * Remove an active controller.
     */
    fun removeController(controller: DiegeticController)
}