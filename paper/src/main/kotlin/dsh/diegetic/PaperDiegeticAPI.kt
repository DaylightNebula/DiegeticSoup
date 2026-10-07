package dsh.diegetic

import dsh.diegetic.controller.DiegeticController
import dsh.diegetic.interop.BukkitPlayer
import dsh.diegetic.interop.DPlayer
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.LinkedList

class PaperDiegeticAPI: DiegeticAPI {
    private val activeControllers = LinkedList<DiegeticController>()
    private val packetAPIInstance = PaperPacketAPI()

    fun init(plugin: JavaPlugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, { _ ->
            activeControllers.toList().forEach(DiegeticController::tick)
        }, 1L, 1L)
    }

    /**
     * Takes IDs from the server's own entity counter, so packet-only displays can never share an ID
     * with a real entity, however long the server has been running.
     */
    override fun nextEntityID(): Int {
        val world = Bukkit.getWorlds().firstOrNull()
            ?: throw IllegalStateException("Diegetic entity IDs can only be allocated once a world has loaded")
        return Bukkit.getUnsafe().nextEntityId(world)
    }
    override fun getPacketAPI() = packetAPIInstance
    override fun getActiveControllers() = activeControllers
    override fun addController(controller: DiegeticController) { activeControllers.add(controller) }
    override fun removeController(controller: DiegeticController) { activeControllers.remove(controller); controller.destroy() }

    override fun getPlayersInWorld(world: String): Collection<DPlayer> {
        val world = Bukkit.getWorld(world) ?: return emptyList()
        return world.players.map { BukkitPlayer(it) }
    }
}