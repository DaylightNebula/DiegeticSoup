package dsh.diegetic

import dsh.diegetic.interop.BukkitPlayer
import dsh.diegetic.interop.DPlayer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.LinkedList

class PaperDiegeticAPI: DiegeticAPI {
    private val activeControllers = LinkedList<DiegeticController>()
    private val packetAPIInstance = PaperPacketAPI()
    private var nextEntityId = 1_000_000

    fun init(plugin: JavaPlugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, { _ ->
            activeControllers.toList().forEach(DiegeticController::tick)
        }, 1L, 1L)
    }

    override fun nextEntityID() = nextEntityId++
    override fun getPacketAPI() = packetAPIInstance
    override fun getActiveControllers() = activeControllers
    override fun addController(controller: DiegeticController) { activeControllers.add(controller) }
    override fun removeController(controller: DiegeticController) { activeControllers.remove(controller); controller.destroy() }

    override fun getPlayersInWorld(world: String): Collection<DPlayer> {
        val world = Bukkit.getWorld(world) ?: return emptyList()
        return world.players.map { BukkitPlayer(it) }
    }
}