package dsh.diegetic

import dsh.diegetic.controller.DiegeticController
import dsh.diegetic.interop.BukkitPlayer
import dsh.diegetic.interop.DPlayer
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.InteractionHand
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity
import dsh.diegetic.interaction.ClickType
import dsh.diegetic.interaction.InteractionRegistry
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.LinkedList

class PaperDiegeticAPI: DiegeticAPI {
    private val activeControllers = LinkedList<DiegeticController>()
    private val packetAPIInstance = PaperPacketAPI()

    fun init(plugin: JavaPlugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, { _ ->
            activeControllers.toList().forEach(DiegeticController::tick)
        }, 1L, 1L)

        // clicks on the invisible interaction entities of UIs; they only exist as packets, so Bukkit never sees them
        PacketEvents.getAPI().eventManager.registerListener(object : PacketListener {
            override fun onPacketReceive(event: PacketReceiveEvent) {
                val (entityId, type) = when (event.packetType) {
                    PacketType.Play.Client.ATTACK -> WrapperPlayClientAttack(event).entityId to ClickType.LEFT
                    PacketType.Play.Client.INTERACT_ENTITY -> {
                        val packet = WrapperPlayClientInteractEntity(event)
                        when (packet.action) {
                            WrapperPlayClientInteractEntity.InteractAction.ATTACK -> packet.entityId to ClickType.LEFT
                            // a right click arrives as INTERACT_AT then INTERACT, once per hand: count the main-hand INTERACT
                            WrapperPlayClientInteractEntity.InteractAction.INTERACT ->
                                if (packet.hand == InteractionHand.MAIN_HAND) packet.entityId to ClickType.RIGHT
                                else { cancelIfOurs(event, packet.entityId); return }
                            else -> { cancelIfOurs(event, packet.entityId); return }
                        }
                    }
                    else -> return
                }
                val player = event.getPlayer<Player>() ?: return
                if (InteractionRegistry.handle(BukkitPlayer(player), entityId, type)) event.isCancelled = true
            }
        }, PacketListenerPriority.NORMAL)
    }

    private fun cancelIfOurs(event: PacketReceiveEvent, entityId: Int) {
        if (InteractionRegistry.isInteraction(entityId)) event.isCancelled = true
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